import java.io.File
import java.net.{HttpURLConnection, URL}
import java.nio.file.Paths

import scala.sys.process._

object Bootstrap {

  val currentFolderPath: String = Paths.get("").toAbsolutePath.toString
  val javaHomePath: String = System.getProperty("java.home")
  val chatOverflowMainClass = "org.codeoverflow.chatoverflow.Launcher"

  def main(args: Array[String]): Unit = {

    println("ChatOverflow Bootstrap Launcher.")

    if (testValidity()) {
      println("Valid ChatOverflow installation. Checking libraries...")

      if (checkLibraries(args)) {
        val javaPath = createJavaPath()
        if (javaPath.isDefined) {
          println("Found java installation. Starting ChatOverflow...")

          // Start chat overflow!
          val process = new java.lang.ProcessBuilder(javaPath.get, "-cp", "bin/*:lib/*", chatOverflowMainClass)
            .inheritIO().start()

          val exitCode = process.waitFor()
          println(s"ChatOverflow stopped with exit code: $exitCode")
        } else {
          println("Unable to find java installation. Unable to start.")
        }
      } else {
        // TODO: Proper management of download problems
        println("Error: Problem with libraries. Unable to start.")
      }
    } else {
      println("Error: Invalid ChatOverflow installation. Please extract all provided files properly. Unable to start.")
    }
  }

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

  def checkLibraries(args: Array[String]): Boolean = {

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

  private def downloadLibrary(libraryName: String, libraryURL: String): Boolean = {
    val url = new URL(libraryURL)

    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(1000)
    connection.setReadTimeout(1000)
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
  }

  private def testValidity(): Boolean = {
    // The only validity check for now is the existence of a bin folder
    new File(currentFolderPath + "/bin").exists()
  }
}
