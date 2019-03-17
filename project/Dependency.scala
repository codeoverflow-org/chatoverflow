import java.net.{HttpURLConnection, URL}

import sbt.internal.util.ManagedLogger

import scala.xml.Node

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
