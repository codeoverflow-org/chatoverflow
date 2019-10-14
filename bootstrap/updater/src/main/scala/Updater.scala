import java.io.{BufferedInputStream, File, FileInputStream}
import java.net.{URL, URLClassLoader}
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.{FileSystemException, Files, Paths}
import java.text.DecimalFormat
import java.util.Date
import java.util.zip.ZipFile

import CLI._
import me.tongfei.progressbar.{ProgressBar, ProgressBarBuilder, ProgressBarStyle}
import org.fusesource.jansi.internal.CLibrary
import org.json4s.jackson.JsonMethods.parse
import org.json4s.{DefaultFormats, Formats}

import scala.collection.JavaConverters._
import scala.io.{Source, StdIn}

/**
 * Contains all logic to update the local installation of ChatOverflow.
 * Uses GitHub releases to find new versions and to download them.
 */
object Updater {
  private val versionFileName = "version.txt"
  private val launcherJar = "bin/ChatOverflow-Launcher.jar"
  private val launcherMainClass = "Bootstrap"
  private var classLoader: URLClassLoader = _
  private val ghBase = "https://api.github.com"
  private val acceptHeader = "Accept" -> "application/vnd.github.v3+json" // ensures that we always get the GitHub v3 API
  private implicit val jsonFormats: Formats = DefaultFormats

  private val repo = "codeoverflow-org/chatoverflow" // Can be changed for testing purposes

  /**
   * Updater entry point.
   * Checks for updates and if available and accepted by the user, installs it.
   * Starts the Bootstrap Launcher
   *
   * @param args arguments for the launcher
   */
  def main(args: Array[String]): Unit = {
    println("Starting ChatOverflow Bootstrap Updater.")

    val conf: Config = ArgsParser.parse(args, Config()) match {
      case Some(value) => value
      case None => System.exit(1); null
    }
    classLoader = getLauncherLoader(conf)

    if (conf.help) {
      // The user just wants to quickly get all available options, don't search for updates to not bother the user.
      // Start the framework through the launcher and simply show the usage.
      startLauncher(Array("--help"), conf)
      return
    }

    if (!conf.ignoreUpdates) {
      println("Checking for updates...")
      val update = Updater.searchForUpdates
      if (update.isDefined) {
        println("A new update is available!")
        println(s"Current version: ${Updater.getCurrentVersion.get}")
        println(s"Newest version: ${update.get.tag_name}")

        // Check if a tty is attached
        if (CLibrary.isatty(CLibrary.STDIN_FILENO) == 1) {
          print(s"Do you want to download and install the update? [y/N] ")
          val in = StdIn.readLine

          if (in.toLowerCase == "y") {
            val file = Updater.downloadUpdate(update.get)
            if (file.isDefined) {
              Updater.installUpdate(file.get, conf)
            }
          }
        } else {
          println("Currently running in a non-interactive session. Please run in an interactive session to auto-update\n" +
            s"or download and install manually from ${update.get.html_url}")
        }
      } else {
        println("No new update is available.")
      }
    } else {
      println("Skipping update check.")
    }

    startLauncher(args, conf)
  }

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
    val zipFile = update.assets.find(e => {
      val n = e.name
      n.endsWith(".zip") && !n.contains("plugin") && !n.contains("dev") // Tries to eliminate zips of the plugin dev environment
    })
    if (zipFile.isEmpty) {
      println("Release doesn't contain a zip file, seems invalid. Skipping update and starting with old version.")
      return None
    }

    println("Downloading update...")

    val url = new URL(zipFile.get.browser_download_url)
    val temp = Files.createTempFile("ChatOverflow", update.tag_name).toFile

    val connection = url.openConnection()
    val pbb = new ProgressBarBuilder()
      .setStyle(ProgressBarStyle.ASCII)
      .setUnit("MB", 1000 * 1000)
      .setInitialMax(zipFile.get.size)
      .showSpeed(new DecimalFormat("0.0"))

    val in = ProgressBar.wrap(connection.getInputStream, pbb)
    try {
      if (temp.exists())
        temp.delete()

      Files.copy(in, temp.toPath)
    } finally {
      in.close()
    }

    println("Update successfully downloaded.")
    Some(temp)
  }

  /**
   * Installs a update from the passed zip file.
   *
   * @param zipFile the zip containing the update
   * @param conf the cli config, used to extract the new files into the correct installation directory
   */
  def installUpdate(zipFile: File, conf: Config): Unit = {
    println("Installing update...")

    val binFiles = new File(s"${conf.directory}/bin").listFiles.filter(_.getName.endsWith(".jar"))
    binFiles.foreach(_.delete())

    classLoader.close() // release locks of jars on windows
    classLoader = null

    // Extract zip
    val zip = new ZipFile(zipFile)
    zip.entries().asScala
      .foreach(entry => {
        val is = zip.getInputStream(entry)
        val out = new File(conf.directory, entry.getName)

        if (out.isDirectory) {
          out.mkdirs()
        } else {
          try {
            Files.copy(is, out.toPath, REPLACE_EXISTING)
          } catch {
            case _: FileSystemException if entry.getName == "ChatOverflow.jar" =>
              // Updater couldn't be updated, because Windows holds file locks on executing files including the updater.
              // Skip update of it, it shouldn't change anyway. We can update it on *nix system in the case we reeeeealy need to.
              // If it has changed and we can't auto-update, we do recommend the user to update the updater manually, but it is to the user to decide.
              val currentIs = new BufferedInputStream(new FileInputStream(s"${conf.directory}/ChatOverflow.jar"))
              val currentHash = Stream.continually(currentIs.read).takeWhile(_ != -1).map(_.toByte).hashCode()
              val newIs = new BufferedInputStream(zip.getInputStream(entry))
              val newHash = Stream.continually(newIs.read).takeWhile(_ != -1).map(_.toByte).hashCode()

              if (currentHash != newHash) {
                println("The ChatOverflow updater has been updated and we can't update it for you when running on Windows.\n" +
                  "It's highly recommended to override the 'ChatOverflow.jar' of your installation with the new version\n" +
                  s"that can be found in the zip file at $zipFile.\n ChatOverflow may still work fine with this version," +
                  "but we can't guarantee that.")
              }
          }
        }

        is.close()
      })

    // Re-set the executable flag for *nix systems
    new File(conf.directory).listFiles()
      .filter(f => f.isFile && f.getName.startsWith("ChatOverflow."))
      .foreach(_.setExecutable(true))

    // Reload all jar files, so that the launcher can be loaded
    classLoader = getLauncherLoader(conf)

    println("Update installed.")
  }

  /**
   * Gets the local installed version, e.g. 0.3-prealpha.
   *
   * @return if successfully the version, otherwise None.
   */
  def getCurrentVersion: Option[String] = {
    val is = classLoader.getResourceAsStream(versionFileName)
    if (is == null)
      None
    else
      Some(Source.fromInputStream(is).getLines().mkString)
  }

  /**
   * Loads the class of the Launcher with the class loader and starts it.
   *
   * @param args the args to pass to the Bootstrap Launcher
   * @param conf cli config, used to check the existence of the launcher jar
   */
  def startLauncher(args: Array[String], conf: Config): Unit = {
    try {
      val cls = classLoader.loadClass(launcherMainClass)
      val mainMethod = cls.getMethod("main", classOf[Array[String]])
      mainMethod.invoke(null, args)
    } catch {
      case _: ClassNotFoundException =>
        if (!new File(s"${conf.directory}/$launcherJar").exists)
          println("Launcher jar is non existent. Seems like your installation is invalid.")
        else
          println(s"Main class of the launcher $launcherMainClass couldn't be found.")
      case _: NoSuchMethodException => println(s"Launcher jar is invalid: couldn't get main method.")
      case e: Throwable => println(s"Launcher jar is invalid: $e");
    }
  }

  private def getLauncherLoader(conf: Config) = new URLClassLoader(Array(
    Paths.get(s"${conf.directory}/$launcherJar").toUri.toURL
  ), getClass.getClassLoader.getParent)

  /**
   * Metadata about a release of ChatOverflow on GitHub.
   * Note that the variables of this class don't represent all the metadata we get by the GitHub API,
   * you can add more metadata by adding a variable with the name that the metadata has in the json object.
   *
   * @param tag_name     the name of the git tag, which this release refers to
   * @param html_url     the url to the release on GitHub
   * @param published_at the date, when this release was published
   * @param assets       all assets attached to this release
   */
  case class Release(tag_name: String, html_url: String, published_at: Date, assets: List[ReleaseAsset])

  /**
   * A file asset of a release.
   *
   * @param name                 the name of the file
   * @param browser_download_url the url, where it can be downloaded
   * @param size                 the size of the file in bytes
   */
  case class ReleaseAsset(name: String, browser_download_url: String, size: Int)

}
