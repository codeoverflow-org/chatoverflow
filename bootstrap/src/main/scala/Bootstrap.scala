import java.io.File
import java.net.{HttpURLConnection, URL, UnknownHostException}
import java.nio.file.Paths

import scala.sys.process._

/**
  * The bootstrap launcher downloads all required libraries and starts chat overflow with the correct parameters.
  */
object Bootstrap {

  // Working directory of the bootstrap launcher
  val currentFolderPath: String = Paths.get("").toAbsolutePath.toString

  // Java home path (jre installation folder)
  val javaHomePath: String = System.getProperty("java.home")

  // Chat Overflow Launcher / Main class (should not change anymore)
  val chatOverflowMainClass = "org.codeoverflow.chatoverflow.Launcher"

  /**
    * Software entry point
    *
    * @param args arguments for the launcher
    */
  def main(args: Array[String]): Unit = {
    println("ChatOverflow Bootstrap Launcher.")

    if (testValidity()) {
      println("Valid ChatOverflow installation. Checking libraries...")

      if (checkLibraries(args)) {
        val javaPath = createJavaPath()
        if (javaPath.isDefined) {
          println("Found java installation. Starting ChatOverflow...")

          // Create config folder, if not existent
          if (!new File("config/").exists()) {
            new File("config/").mkdir()
          }
          // TODO: Fix chat overflow config service to handle non existent config folder. I mean... what?

          // Start chat overflow!
          val process = new java.lang.ProcessBuilder(javaPath.get, "-cp", s"bin/*${File.pathSeparator}lib/*", chatOverflowMainClass)
            .inheritIO().start()

          val exitCode = process.waitFor()
          println(s"ChatOverflow stopped with exit code: $exitCode")
        } else {
          println("Unable to find java installation. Unable to start.")
        }
      } else {
        println("Error: Problem with libraries. Unable to start.")
      }
    } else {
      println("Error: Invalid ChatOverflow installation. Please extract all provided files properly. Unable to start.")
    }
  }

  /**
    * Takes the java home path of the launcher and tries to find the java(.exe)
    *
    * @return the path to the java runtime or none, if the file was not found
    */
  def createJavaPath(): Option[String] = {

    // Check validity of java.home path first
    if (!new File(javaHomePath).exists()) {
      None
    } else {

      // Check for windows and unix java versions
      // TODO: How to handle JDK versions? Only JRE supported?
      val javaExePath = s"$javaHomePath/bin/java.exe"
      val javaPath = s"$javaHomePath/bin/java"

      if (new File(javaExePath).exists()) {
        Some(javaExePath)
      } else if (new File(javaPath).exists()) {
        Some(javaPath)
      } else {
        None
      }
    }

  }

  /**
    * Checks if the library folder exists or the reload-flag is set. Triggers the download-process.
    *
    * @param args the args, the launcher has been called with
    * @return false, if there is a serious problem
    */
  def checkLibraries(args: Array[String]): Boolean = {

    // TODO: Someday in the future, we need incremental library checking to manage updates without full download

    val libFolder = new File(s"$currentFolderPath/lib")
    // Args contains --reload or lib folder is non existent?
    if ((args.length > 0 && args.head == "--reload") || !libFolder.exists()) {

      // Create or clean directory
      if (libFolder.exists()) {
        for (libFile <- libFolder.listFiles()) {
          try {
            libFile.delete()
          } catch {
            case e: Exception => println(s"Unable to delete file '${libFile.getName}'. Message: ${e.getMessage}")
          }
        }
      } else {
        try {
          libFolder.mkdir()
        } catch {
          case e: Exception => println(s"Unable to create library directory. Message: ${e.getMessage}")
        }
      }

      // Download all libraries
      // TODO: Check validity if everything is downloaded
      println("Downloading libraries...")
      downloadLibraries()

    } else {
      println("Found libraries folder. Assuming all dependencies are available properly.")
      true
    }
  }

  /**
    * Reads the dependency xml file and tries to download every library.
    *
    * @return false, if there is a serious problem
    */
  def downloadLibraries(): Boolean = {

    // Get dependency xml and read dependencies with their download URL
    val dependencyStream = getClass.getResourceAsStream("/dependencies.xml")
    val dependencyXML = xml.XML.load(dependencyStream)
    val dependencies = for (dependency <- dependencyXML \\ "dependency")
      yield ((dependency \ "name").text.trim, (dependency \ "url").text.trim)

    for (i <- dependencies.indices) {
      val dependency = dependencies(i)
      println(s"[${i + 1}/${dependencies.length}] ${dependency._1} (${dependency._2})")
      if (!downloadLibrary(dependency._1, dependency._2)) {
        // Second try, just in case
        downloadLibrary(dependency._1, dependency._2)
      }
    }
    true
  }

  /**
    * Downloads a specified library
    */
  private def downloadLibrary(libraryName: String, libraryURL: String): Boolean = {
    val url = new URL(libraryURL)

    try {
      val connection = url.openConnection().asInstanceOf[HttpURLConnection]
      connection.setConnectTimeout(3000)
      connection.setReadTimeout(3000)
      connection.connect()

      if (connection.getResponseCode >= 400) {
        println("Error: Unable to download library.")
        false
      }
      else {
        // Save file in the lib folder (keeping the name and type)
        try {
          url #> new File(s"$currentFolderPath/lib/${libraryURL.substring(libraryURL.lastIndexOf("/"))}") !!

          true
        } catch {
          case e: Exception =>
            println(s"Error: Unable to save library. Message: ${e.getMessage}")
            false
        } finally {
          connection.disconnect()
        }
      }
    } catch {
      case e: UnknownHostException =>
        println(s"Error. Unable to connect to the url '$url'. Message: ${e.getMessage}")
        false
    }
  }

  /**
    * Checks, if the installation is valid
    */
  private def testValidity(): Boolean = {
    // The only validity check for now is the existence of a bin folder
    new File(currentFolderPath + "/bin").exists()
  }
}
