import java.io.File
import java.net.{HttpURLConnection, URL}

import sbt.internal.util.ManagedLogger

import scala.xml.{Node, XML}

class Dependency(dependencyString: String, logger: ManagedLogger) {
  var nameWithoutScalaVersion = ""
  var version = ""
  var url = ""
  var available = false
  create()

  override def toString: String = {
    s"$nameWithoutScalaVersion ($version) - $available - $url"
  }

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

object BootstrapUtility {
  val dependencyListFileName = "dependencyList.txt"
  val dependencyXMLFileName = "bootstrap/src/main/resources/dependencies.xml"

  def bootstrapGenTask(logger: ManagedLogger, scalaLibraryVersion: String): Unit = {
    println("Welcome to the bootstrap generation utility. It's time to build!")

    // Dependency management
    val dependencyList = retrieveDependencies(logger, scalaLibraryVersion)
    saveDependencyXML(dependencyList, logger)

    // Copy built jar files
    copyJars(logger)
  }

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

  private def copyJars(logger: ManagedLogger): Unit = {
  }

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

}