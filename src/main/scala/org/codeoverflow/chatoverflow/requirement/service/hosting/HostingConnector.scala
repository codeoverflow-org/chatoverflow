package org.codeoverflow.chatoverflow.requirement.service.hosting

import java.io.{File, FileInputStream}

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector

import scala.collection.mutable

class HostingConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {
  override protected var requiredCredentialKeys: List[String] = List()
  override protected var optionalCredentialKeys: List[String] = List()

  private val entities = new mutable.HashMap[String, File]()

  def addHostedEntity(file: File, endpoint: String): Boolean = {
    if (hostedEntityExists(endpoint)) {
      false
    } else {
      entities += endpoint -> file
      true
    }
  }

  def removeHostedEntity(endpoint: String): Boolean = {
    if (!hostedEntityExists(endpoint)) {
      false
    } else {
      entities -= endpoint
      true
    }
  }

  def getHostedEntityFileStream(endpoint: String): Option[FileInputStream] = {
    if (!hostedEntityExists(endpoint)) {
      None
    } else {
      Some(new FileInputStream(entities(endpoint)))
    }
  }

  def hostedEntityExists(endpoint: String): Boolean = entities.contains(endpoint)

  /**
    * Starts the connector, e.g. creates a connection with its platform.
    */
  override def start(): Boolean = {
    logger info s"Started hosting connector for sub directory '$sourceIdentifier'."
    true
  }

  /**
    * This stops the activity of the connector, e.g. by closing the platform connection.
    */
  override def stop(): Boolean = {
    logger info s"Stopped hosting connector for sub directory '$sourceIdentifier'. Hosted ${entities.size} entities."
    true
  }
}
