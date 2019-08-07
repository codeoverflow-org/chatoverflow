import java.net.{HttpURLConnection, URL}

import sbt.internal.util.ManagedLogger

/**
  * Holds logic to resolve dependencies with their author, name and version.
  * To be used by the BootstrapUtility to get urls for dependencies from dependencyList.txt
  */
object DependencyResolver {
  // Contains all repos with descending priority.
  // All urls MUST have the trailing slash.
  private val repos = Set(
    "http://central.maven.org/maven2/", // used by almost anything
    "http://jcenter.bintray.com/" // used by JDA and its dependencies like opus-java
  )

  /**
    * Resolves the given dependency into a url, where it can be downloaded.
    * Uses the private "repos" constant to determine possible urls.
    *
    * @param dependency the dependency to resolve. The tuple needs to consist of following
    *                   1. the author or organisation
    *                   2. the name with the scala version if needed
    *                   3. the version
    * @param logger     the sbt logger for exception warnings
    * @return the resolved url if everything went well.
    *         In case of an error that persists after 3 tries
    *         the Option is empty and the error message has been logged.
    */
  def resolve(dependency: (String, String, String), logger: ManagedLogger): Option[String] = {
    for (repo <- repos) {
      val url = buildUrl(dependency, repo)
      val status = testURL(url, 1, 3)

      status match {
        // Repo has produced 3 errors in a row! Give up, report error and move on to the next repo
        case Left(e) => logger warn s"Error while testing dependency ${dependency.productIterator.mkString(":")}" +
          s" availability at $url: ${e.getMessage}"

        // Repo returned with a 200 OK, the url is correct and we don't need to check the following repos
        // If found is false e.g. by 404 Not Found we check the next repo for this dependency
        case Right(found) => if (found) return Some(url)
      }
    }

    None // No repo has this dependency, can't resolve it
  }

  /**
    * Generates a default url for the provided dependency using the primary/first repo (Maven Central).
    * It doesn't check whether this url actually works and is designed to be used as a fallback url.
    *
    * @param dependency the dependency for which to get the default url
    * @return the default url of this dependency
    */
  def getDefaultUrl(dependency: (String, String, String)): String = buildUrl(dependency, repos.head)

  /**
    * Builds the url from the repo base url and the dependency tuple.
    *
    * @param dependency the dependency to convert to a url and append to the repo root
    * @param repo       the repo root with a trailing slash
    * @return the fully build url
    */
  private def buildUrl(dependency: (String, String, String), repo: String): String = {
    val (author, name, version) = dependency

    s"$repo${author.replaceAll("\\.", "/")}/$name/$version/$name-$version.jar"
  }

  /**
    * Tests if the given url is available and returns 200 OK as http status code.
    * Tries multiple times using recursion if a error occurs. Maximal retry count depends on recursionLimit.
    *
    * @param url            the url to be checked
    * @param recursionCount current retry count, should be 1 if called from anything else than itself
    * @param recursionLimit maximal retries
    * @return Either a exception in case a exception is thrown and the recursionLimit is reached
    *         or a boolean if no exception was thrown.
    *         True if the http status code was 200 and false otherwise.
    */
  private def testURL(url: String, recursionCount: Int, recursionLimit: Int): Either[Exception, Boolean] = {
    var status = -1

    try {

      // Test if the url exists
      val connection = new URL(url).openConnection.asInstanceOf[HttpURLConnection]
      connection.setRequestMethod("HEAD")
      connection.setConnectTimeout(400) // JCenter is very slow with 404s for some reason
      connection.setReadTimeout(400)
      status = connection.getResponseCode
      connection.disconnect()

    } catch {
      case e: Exception =>
        return if (recursionCount < recursionLimit) {
          testURL(url, recursionCount + 1, recursionLimit)
        } else {
          Left(e)
        }
    }

    Right(status == 200)
  }
}
