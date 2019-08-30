import java.io.File
import java.util.Date

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.io.Source

/**
 * Contains all logic to update the local installation of ChatOverflow.
 * Uses GitHub releases to find new versions and to download them.
 */
object Updater {
  private val versionFileName = "/version.txt"
  private val httpClient = HttpClientBuilder.create.build
  private val ghBase = "https://api.github.com"
  private val acceptHeader = new BasicHeader("Accept", "application/vnd.github.v3+json")
  private implicit val jsonFormats: Formats = DefaultFormats

  private val repo = "codeoverflow-org/chatoverflow" // Can be changed for testing purposes

  /**
   * Searches for any updates that are newer than the local version.
   *
   * @return A release, if it is newer than the local version, otherwise None.
   */
  def searchForUpdates: Option[Release] = {
    val version = getCurrentVersion
    if (version.isEmpty) {
      println("Couldn't determine current ChatOverflow version. Skipping update check.")
      return None
    }

    val releases = getReleases

    if (releases.isEmpty || releases.get.isEmpty) {
      println("Couldn't get releases on GitHub. Skipping update check.")
      return None
    }

    val current = releases.get.find(_.tag_name == version.get)
    if (current.isEmpty) {
      println(s"Couldn't find used release '${version.get}' on GitHub. Skipping update check.")
      return None
    }

    val latest = releases.get.maxBy(_.published_at.getTime)

    if (latest.published_at.after(current.get.published_at))
      Some(latest)
    else
      None
  }

  /**
   * Gets all releases from GitHub using the GitHub API.
   *
   * @return None, if something has failed. A list of all releases otherwise.
   */
  def getReleases: Option[List[Release]] = {
    val url = s"$ghBase/repos/$repo/releases"
    val request = new HttpGet(url)
    request.addHeader(acceptHeader)

    val entity = httpClient.execute(request).getEntity

    try {
      Option(entity)
        .map(EntityUtils.toString)
        .flatMap(str => parse(str).extractOpt[List[Release]])
    } catch {
      case e: Throwable =>
        println(e)
        None
    }
  }

  def downloadUpdate(update: Release): Option[File] = ???

  def installUpdate(zipFile: File): Boolean = ???

  def restartBootstrapLauncher(): Unit = ???

  /**
   * Gets the local installed version, e.g. 0.3-prealpha.
   *
   * @return if successfully the version, otherwise None.
   */
  def getCurrentVersion: Option[String] = {
    val is = getClass.getResourceAsStream(versionFileName)
    if (is == null)
      None
    else
      Some(Source.fromInputStream(is).getLines().mkString)
  }

  /**
   * Metadata about a release of ChatOverflow on GitHub.
   * Note that the variables of this class doesn't represent all the metadata we get by the GitHub API,
   * you can add more metadata by adding a variable with the name that the metadata has in the json object.
   *
   * @param tag_name the name of the git tag, which this release refers to
   * @param html_url the url to the release on GitHub
   * @param published_at the date, when this release was published
   * @param assets all assets attached to this release
   */
  case class Release(tag_name: String, html_url: String, published_at: Date, assets: List[ReleaseAsset])

  /**
   * A file asset of a release.
   *
   * @param name the name of the file
   * @param browser_download_url the url, where it can be downloaded
   * @param size the size of the file in bytes
   */
  case class ReleaseAsset(name: String, browser_download_url: String, size: Int)

}
