import java.io.File
import java.net.{HttpURLConnection, URL}
import java.nio.file.{Files, Paths}

import sbt.internal.util.ManagedLogger

import scala.xml.{Node, XML}

/**
  * A dependency holds all information of a library like name, version and maven url.
  *
  * @param dependencyString the input string from the 'dependencyList' sbt command
  * @param logger           the sbt logger
  */
class Dependency(dependencyString: String, logger: ManagedLogger) {
  var nameWithoutScalaVersion = ""
  var version = ""
  var url = ""
  var available = false
  create()

  override def toString: String = {
    s"$nameWithoutScalaVersion ($version) - $available - $url"
  }

  /**
    * Converts the dependency to its xml representation, ready to be saved.
    *
    * @return a xml node called 'dependency'
    */
  def toXML: Node = {
    <dependency>
      <name>
        {nameWithoutScalaVersion}
      </name>
      <version>
        {version}
      </version>
      <url>
        {url}
      </url>
    </dependency>
  }

  /**
    * This constructor-alike function reads the console output of the 'dependencyList' sbt command
    * and fills all required information into the dependency object
    */
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

        available = testURL(0, 3)

      case _ =>
        logger warn s"Invalid dependency format: '$dependencyString'."
    }
  }

  /**
    * Tests, if the dependency url is available. Uses recursion to handle connection faults.
    */
  private def testURL(recursionCount: Int, recursionLimit: Int): Boolean = {

    var status = -1

    try {

      // Test if the url exists
      val connection = new URL(url).openConnection.asInstanceOf[HttpURLConnection]
      connection.setRequestMethod("HEAD")
      connection.setConnectTimeout(200)
      connection.setReadTimeout(200)
      status = connection.getResponseCode
      connection.disconnect()

    } catch {
      case e: Exception => logger warn s"Error while testing dependency (attempt $recursionCount of $recursionLimit)" +
        s" availability of ${this}: ${e.getMessage}"
    }

    if (status != 200 && recursionCount + 1 <= recursionLimit) {
      testURL(recursionCount + 1, recursionLimit)
    } else {
      status == 200
    }
  }

}

/**
  * Holds the functionality to read all dependencies and feed the bootstrap launcher with this information.
  * Should be used once for every new public version of chat overflow.
  */
object BootstrapUtility {
  val dependencyListFileName = "dependencyList.txt"
  val dependencyProjectBasePath = "bootstrap/src/main"
  val dependencyXMLFileName = s"$dependencyProjectBasePath/resources/dependencies.xml"

  /**
    * This task retrieves all dependencies and creates a xml file for the bootstrap launcher.
    *
    * @param logger              the sbt logger
    * @param scalaLibraryVersion the current scala library version
    */
  def bootstrapGenTask(logger: ManagedLogger, scalaLibraryVersion: String): Unit = {
    println("Welcome to the bootstrap generation utility. It's time to build!")

    // Dependency management
    val dependencyList = retrieveDependencies(logger, scalaLibraryVersion)
    saveDependencyXML(dependencyList, logger)

    println("Finished bootstrap generation utility. Have a nice day!")
  }

  /**
    * Saves all dependencies as XML in the bootstrap launcher dependency xml file.
    */
  private def saveDependencyXML(dependencyList: List[Dependency], logger: ManagedLogger): Unit = {

    logger info "Started saving dependency XML."

    val xml =
      <dependencies>
        {for (dependency <- dependencyList) yield dependency.toXML}
      </dependencies>

    logger info "Saving dependency XML now."
    XML.save(dependencyXMLFileName, xml)
    logger info "Finished saving XML file."
  }

  /**
    * Uses a file called 'dependencyList.txt' with the output of the
    * sbt command 'dependencyList' to retrieve all dependencies.
    */
  private def retrieveDependencies(logger: ManagedLogger, scalaLibraryVersion: String): List[Dependency] = {

    logger info "Starting dependency retrieval."

    val dependencyFile = new File(dependencyListFileName)

    if (!dependencyFile.exists()) {
      logger error "No dependency file found. Please copy the output of the task 'dependencyList' into a file named 'dependencyList.txt' in the root folder."
      List[Dependency]()
    } else {

      logger info "Found dependency file."

      // Load file, remove the info tag and create dependency objects
      val input = scala.io.Source.fromFile(dependencyFile).getLines().toList
      val lines = input.map(line => line.replaceFirst("\\[info\\] ", ""))

      logger info "Read dependencies successfully. Creating dependency list."

      val dependencies = for (line <- lines) yield new Dependency(line, logger)

      logger info "Updating and modifying dependencies..."

      // Modify dependencies: Remove ChatOverflow, add scala library
      val depsWithoutChatOverflow = dependencies.filter(d =>
        d.nameWithoutScalaVersion != "chatoverflow" && d.nameWithoutScalaVersion != "chatoverflow-api")
      val modifiedDependencies = depsWithoutChatOverflow ++
        List(new Dependency(s"org.scala-lang:scala-library:$scalaLibraryVersion", logger))

      // Info output
      logger info s"Found ${modifiedDependencies.length} dependencies."
      if (modifiedDependencies.exists(d => !d.available)) {
        logger warn "Found the following dependencies, that could not be retrieved online:"
        logger warn modifiedDependencies.filter(d => !d.available).map(_.toString).mkString("\n")
      }

      modifiedDependencies
    }
  }

  /**
    * Prepares the environemnt for deployment. Should be called after package and assembly task.
    *
    * @param logger              the sbt logger
    * @param scalaLibraryVersion the scala library major version
    */
  def prepareDeploymentTask(logger: ManagedLogger, scalaLibraryVersion: String): Unit = {
    // Assuming, before this: clean, bs, assembly bootstrapProject, package
    // Assuming: Hardcoded "bin/" and "deploy/" folders
    // Assuming: A folder called "deployment-files" with all additional files (license, bat, etc.)
    logger info "Started deployment process."

    // First step: Preparing bin folder
    logger info "Preparing 'bin/' folder."
    createOrEmptyFolder("bin/")

    // Second step: Preparing deploy folder, copying bin folder, bootstrap launcher, etc.
    logger info "Preparing 'deploy/' folder."
    createOrEmptyFolder("deploy/")
    createOrEmptyFolder("deploy/bin/")

    // Third step: Copying chat overflow files
    logger info "Copying chat overflow files..."

    val sourceJarDirectories = List(s"target/scala-$scalaLibraryVersion/",
      s"api/target/scala-$scalaLibraryVersion/")

    val targetJarDirectories = List("bin", "deploy/bin")

    for (sourceDirectory <- sourceJarDirectories) {
      copyJars(sourceDirectory, targetJarDirectories, logger)
    }

    // Fourth step: Copy bootstrap launcher
    copyJars(s"bootstrap/target/scala-$scalaLibraryVersion/", List("deploy/"), logger)

    // Last step: Copy additional files
    logger info "Copying additional deployment files..."
    val deploymentFiles = new File("deployment-files/")
    if (!deploymentFiles.exists()) {
      logger warn "Unable to find deployment files."
    } else {
      for (deploymentFile <- deploymentFiles.listFiles()) {
        Files.copy(Paths.get(deploymentFile.getAbsolutePath),
          Paths.get(s"deploy/${deploymentFile.getName}"))
        logger info s"Finished copying additional deployment file '${deploymentFile.getName}'."
      }
    }

    // TODO: Deployment readme file html
    // TODO: Deployment bat file
    // TODO: Deployment unix launch file (...?)
  }

  /**
    * Creates a directory or empties it, by recursively deleting files and sub directories.
    */
  private def createOrEmptyFolder(path: String): Unit = {
    val folder = new File(path)
    if (folder.exists()) {
      for (file <- folder.listFiles()) {
        if (file.isFile) {
          file.delete()
        } else {
          createOrEmptyFolder(file.getAbsolutePath)
          file.delete()
        }
      }
    } else {
      folder.mkdir()
    }
  }

  /**
    * Copies ONE jar file from the source to all target directories. Useful for single packaged jar files.
    */
  private def copyJars(sourceDirectory: String, targetDirectories: List[String], logger: ManagedLogger): Unit = {
    val candidates = new File(sourceDirectory)
      .listFiles().filter(f => f.isFile && f.getName.toLowerCase.endsWith(".jar"))
    if (candidates.length != 1) {
      logger warn s"Unable to identify jar file in $sourceDirectory"
    } else {
      for (targetDirectory <- targetDirectories) {
        Files.copy(Paths.get(candidates.head.getAbsolutePath),
          Paths.get(s"$targetDirectory/${candidates.head.getName}"))
        logger info s"Finished copying file '${candidates.head.getAbsolutePath}' to '$targetDirectory'."
      }
    }
  }
}

// TODO: Use withTaskInfo to properly print tasks
// TODO: Create missing deployment files
// TODO: Create and commit deploy run configuration. clean, package, bs, assembly (bootstrapProject), deploy