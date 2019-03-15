import java.io.File
import java.net.{HttpURLConnection, URL}
import java.nio.file.Paths

import scala.sys.process._

object Bootstrap {

  val currentFolderPath: String = Paths.get("").toAbsolutePath.toString
  val javaHomePath: String = System.getProperty("java.home")
  val chatOverflowMainClass = "org.codeoverflow.chatoverflow.Launcher"

  def main(args: Array[String]): Unit = {

    if (testValidity()) {
      if (checkLibraries(args)) {
        val javaPath = createJavaPath()
        if (javaPath.isDefined) {

          // Start chat overflow!
          val process = new java.lang.ProcessBuilder(javaPath.get, "-cp", "bin/*:lib/*", chatOverflowMainClass)
            .inheritIO().start()

          val exitCode = process.waitFor()
          println(s"ChatOverflow stopped with code: $exitCode")
        }
      }
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
          libFile.delete()
        }
      } else {
        libFolder.mkdir()
      }

      // Download all libraries
      // TODO: Check validity if everything is downloaded
      downloadLibraries()

    } else {
      true
    }
  }

  def downloadLibraries(): Boolean = {

    // Get dependency xml and read dependencies with their download URL
    val dependencyStream = getClass.getResourceAsStream("/dependencies.xml")
    val dependencyXML = xml.XML.load(dependencyStream)
    val dependencies = for (dependency <- dependencyXML \\ "dependency")
      yield ((dependency \ "name").text.trim, (dependency \ "url").text.trim)

    for ((name, url) <- dependencies) {
      downloadLibrary(name, url)
    }

    true
  }

  private def downloadLibrary(libraryName: String, libraryURL: String): Unit = {
    val url = new URL(libraryURL)

    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(5000)
    connection.setReadTimeout(5000)
    connection.connect()

    if (connection.getResponseCode >= 400) {

    }
    // TODO: Retry 3 times
    // TODO: Feedback on download progress
    else {

      // TODO: try
      url #> new File(s"$currentFolderPath/lib/${libraryURL.substring(libraryURL.lastIndexOf("/"))}") !!
    }
  }

  private def testValidity(): Boolean = {
    // The only validity check for now is the existence of a bin folder
    new File(currentFolderPath + "/bin").exists()
  }

  /*
  TODO: Code deploy task (copying files, used after sbt clean and assembly)
  TODO: Code bootstrap launcher
  1. Bootstrap launcher checks integrity (bin folder existing)
  2. Bootstrap launcher checks libraries (no lib folder or flag -> Download everything
  3. Bootstrap launcher checks java path launched with and starts java -cp "..." chat overflow main class
   */

}
