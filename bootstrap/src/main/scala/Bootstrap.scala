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
    * Checks if the library folder exists or the reload-flag is set. Triggers the download-process if libraries are missing.
    *
    * @param args the args, the launcher has been called with
    * @return false, if there is a serious problem
    */
  def checkLibraries(args: Array[String]): Boolean = {

    val libFolder = new File(s"$currentFolderPath/lib")

    // Create folder for libs if missing
    if (!libFolder.exists()) {
      try {
        libFolder.mkdir()
      } catch {
        case e: Exception => println(s"Unable to create library directory. Message: ${e.getMessage}")
          return false
      }
    }

    // --reload flags instructs to delete all downloaded libraries and to re-download them
    if (args.contains("--reload")) {
      for (libFile <- libFolder.listFiles()) {
        try {
          libFile.delete()
        } catch {
          case e: Exception => println(s"Unable to delete file '${libFile.getName}'. Message: ${e.getMessage}")
            return false
        }
      }
    }

    val dependencies = getDependencies

    // Download all libraries
    // TODO: Check validity if everything is downloaded
    // try downloading libs and only if it succeeded (returned true) then try to delete older libs
    downloadMissingLibraries(dependencies) && deleteUndesiredLibraries(dependencies)
  }

  /**
    * Reads the dependency xml file and tries to download every library that is missing.
    *
    * @return false, if there is a serious problem
    */
  private def downloadMissingLibraries(dependencies: List[(String, String)]): Boolean = {
    // using par here to make multiple http requests in parallel, otherwise its awfully slow on internet connections with high RTTs
    val missing = dependencies.par.filterNot(dep => isLibraryDownloaded(dep._2)).toList

    if (missing.isEmpty) {
      println("All required libraries are already downloaded.")
    } else {
      println(s"Downloading ${missing.length} missing libraries...")

      for (i <- missing.indices) {
        val (name, url) = missing(i)

        println(s"[${i + 1}/${missing.length}] $name ($url)")
        if (!downloadLibrary(name, url)) {
          // Second try, just in case
          if (!downloadLibrary(name, url)) {
            return false // error has been displayed, stop bootstrapper from starting with missing lib
          }
        }
      }
    }
    true // everything went fine
  }

  /**
    * Deletes all undesired libraries. Currently these are all libs that aren't on the list of dependencies.
    * The main responsibility is to delete old libs that got updated or libs that aren't required anymore by Chat Overflow.
    *
    * @param dependencies the libs that should be kept
    * @return false, if a file couldn't be deleted
    */
  private def deleteUndesiredLibraries(dependencies: List[(String, String)]): Boolean = {
    val libDir = new File(s"$currentFolderPath/lib")
    if (libDir.exists() && libDir.isDirectory) {
      // Desired filenames
      val libraryFilenames = dependencies.map(d => libraryFile(d._2).getName)

      val undesiredFiles = libDir.listFiles().filterNot(file => libraryFilenames.contains(file.getName)) // filter out libs on the dependency list

      // Count errors while trying to remove undesired files
      val errorCount = undesiredFiles.count(file => {
        println(s"Deleting old or unnecessary library at $file")
        if (file.delete()) {
          false // no error
        } else {
          println(s"Error: Couldn't delete file $file.")
          true // error
        }
      })
      errorCount == 0 // return false if at least one error occurred
    } else {
      // Shouldn't be possible, because this is called from checkLibraries, which creates this directory.
      true
    }
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
          url #> libraryFile(libraryURL) !!

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
    * Gets all required dependencies from the dependencies.xml in the jar file
    *
    * @return a list of tuples that contain the name (e.g. log4j) without org or version and the url.
    */
  private def getDependencies: List[(String, String)] = {
    val stream = getClass.getResourceAsStream("/dependencies.xml")
    val depXml = xml.XML.load(stream)
    val dependencies = depXml \\ "dependency"
    val dependencyTuples = dependencies.map(dep => {
      val name = (dep \ "name").text.trim
      val url = (dep \ "url").text.trim
      (name, url)
    })

    dependencyTuples.toList
  }

  /**
    * Checks whether this library is fully downloaded
    *
    * @param libraryURL the url of the library
    * @return true if it is completely downloaded, false if only partially downloaded or not downloaded at all
    */
  private def isLibraryDownloaded(libraryURL: String): Boolean = {
    val f = libraryFile(libraryURL)

    if (!f.exists()) {
      false
    } else {
      try {
        // We assume here that the libs don't change at the repo.
        // While this is true for Maven Central, which is immutable once a file has been uploaded, its not for JCenter.
        // Updating a released artifact generally isn't valued among developers
        // and the odds of the updated artifact having the same size is very unlikely.
        val url = new URL(libraryURL)
        url.openConnection().getContentLengthLong == f.length()
      } catch {
        case _: Exception => false
      }
    }
  }

  private def libraryFile(libraryURL: String): File = {
    new File(s"$currentFolderPath/lib/${libraryURL.substring(libraryURL.lastIndexOf("/"))}")
  }

  /**
    * Checks, if the installation is valid
    */
  private def testValidity(): Boolean = {
    // The only validity check for now is the existence of a bin folder
    new File(currentFolderPath + "/bin").exists()
  }
}
