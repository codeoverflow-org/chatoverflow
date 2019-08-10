package org.codeoverflow.chatoverflow.build.deployment

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

    dependencyString match {
      case DependencyRegex(depAuthor, depName, scalaVersion, depVersion) =>
        this.nameWithoutScalaVersion = depName
        this.version = depVersion

        val combinedName = if (scalaVersion != null) depName + scalaVersion else depName
        val depTuple = (depAuthor, combinedName, depVersion)

        val status = DependencyResolver.resolve(depTuple, logger)

        // Provide the url of the default repo if the dependency couldn't be resolved
        url = status.getOrElse(DependencyResolver.getDefaultUrl(depTuple))
        available = status.isDefined

      case _ =>
        logger warn s"Invalid dependency format: '$dependencyString'."
    }
  }

}
