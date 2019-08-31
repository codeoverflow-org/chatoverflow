import java.io.File
import java.net.URL
import java.nio.file.Files
import java.util.Date
import java.util.zip.ZipFile

import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.collection.JavaConverters._
import scala.io.Source
import scala.sys.process._

/**
 * Contains all logic to update the local installation of ChatOverflow.
 * Uses GitHub releases to find new versions and to download them.
 */
object Updater {
  private val versionFileName = "/version.txt"
  private val ghBase = "https://api.github.com"
  private val acceptHeader = "Accept" -> "application/vnd.github.v3+json"
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
    val url = new URL(s"$ghBase/repos/$repo/releases")
    val conn = url.openConnection()

    val (key, value) = acceptHeader
    conn.setRequestProperty(key, value)

    try {
      val body = Source.fromInputStream(conn.getInputStream).mkString
      parse(body).extractOpt[List[Release]]
    } catch {
      case e: Throwable =>
        println(e)
        None
    }
  }

  /**
   * Tries to download the zip of the passed release to a temp file.
   *
   * @param update the release which should be downloaded
   * @return None, if no zip file is attached to the release. When successful returns the temp file.
   */
  def downloadUpdate(update: Release): Option[File] = {
    val zipFile = update.assets.find(_.name.endsWith(".zip"))
    if (zipFile.isEmpty) {
      println("Release doesn't contain a zip file, seems invalid. Skipping update and starting with old version.")
      return None
    }

    println("Downloading update...")

    val url = new URL(zipFile.get.browser_download_url)
    val temp = Files.createTempFile("ChatOverflow", update.tag_name).toFile

    url #> temp !!

    println("Update successfully downloaded")
    Some(temp)
  }

  /**
   * Installs a update from the passed zip file.
   *
   * @param zipFile the zip containing the update
   */
  def installUpdate(zipFile: File): Unit = {
    println("Installing update...")

    val binFiles = new File("bin").listFiles.filter(_.getName.endsWith(".jar"))
    binFiles.foreach(_.delete())

    val zip = new ZipFile(zipFile)
    zip.entries().asScala.foreach(entry => {
      val is = zip.getInputStream(entry)
      val out = new File(".", entry.getName)

      if (out.isDirectory) {
        out.mkdirs()
      } else {
        if (out.exists())
          out.delete()

        Files.copy(is, out.toPath)
      }

      is.close()
    })

    println("Update installed.")
  }

  /**
   * Restarts the Bootstrap Launcher inorder to use the new version
   *
   * @param args the args to pass to the bootstrap launcher
   */
  def restartBootstrapLauncher(args: Array[String]): Unit = {
    println("Restarting Bootstrap Launcher to use new version.")

    val javaPath = Bootstrap.createJavaPath()
    if (javaPath.isDefined) {
      val command = List(javaPath.get, "-jar", "ChatOverflow.jar") ++ args
      new java.lang.ProcessBuilder(command: _*)
        .inheritIO().start().waitFor()
    } else {
      println("Can't automatically restart Bootstrap Launcher. Please do it yourself.")
    }
  }

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
