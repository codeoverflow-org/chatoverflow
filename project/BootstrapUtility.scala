import java.net.{HttpURLConnection, URL}

import sbt.internal.util.ManagedLogger

class Dependency(dependencyString: String, logger: ManagedLogger) {
  var nameWithoutScalaVersion = ""
  var version = ""
  var url = ""
  var available = false
  create()

  override def toString: String = {
    s"$nameWithoutScalaVersion ($version) - $available - $url"
  }

  private def create(): Unit = {
    val DependencyRegex = "([^:]+):([^:_]+)(_[^:]+)?:([^:]+)".r
    val mavenCentralFormat = "http://central.maven.org/maven2/%s/%s/%s/%s.jar"

    dependencyString match {
      case DependencyRegex(depAuthor, depName, scalaVersion, depVersion) =>
        this.nameWithoutScalaVersion = depName
        this.version = depVersion

        val combinedName = if (scalaVersion != null) depName + scalaVersion else depName

        // Create URL for maven central
        url = mavenCentralFormat.format(depAuthor.replaceAll("\\.", "/"), s"$combinedName",
          depVersion, s"$combinedName-$depVersion")

        // Test if the url exists
        val connection = new URL(url).openConnection.asInstanceOf[HttpURLConnection]
        connection.setRequestMethod("HEAD")
        connection.setConnectTimeout(5000)
        connection.setReadTimeout(5000)
        val status = connection.getResponseCode
        connection.disconnect()
        available = status == 200
      case _ =>
        logger warn s"Invalid dependency format: '$dependencyString'."
    }
  }

}

object BootstrapUtility {

  def bootstrapDependencyGenTask(logger: ManagedLogger): Unit = {
    println("Welcome to the bootstrap generation utility. It's time to build!")
    println("Please enter the console output of the task 'dependencyList' without the intro part (so only the dependencies).")
    println("Should look like '[info] ... [info] ...'")
    println("> ")

    // We just assume that the building guy knows what he's doing, lol
    val input = scala.io.Source.fromInputStream(System.in).bufferedReader().readLine()

    // Splits the lines at the info tag of the console output
    val lines = input.replaceFirst("\\[info\\] ", "").split(" \\[info\\] ")
    val dependencies = for (line <- lines) yield new Dependency(line, logger)

    // Modify dependencies: Remove ChatOverflow, add scala library
  }


}