package org.codeoverflow.chatoverflow.requirement.service.sample

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector

class SampleConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {
  override protected var requiredCredentialKeys: List[String] = List()
  override protected var optionalCredentialKeys: List[String] = List()

  /**
    * Starts the connector, e.g. creates a connection with its platform.
    */
  override def start(): Boolean = {
    logger info "Started sample connector!"
    true
  }

  /**
    * This stops the activity of the connector, e.g. by closing the platform connection.
    */
  override def stop(): Boolean = {
    logger info "Stopped sample connector!"
    true
  }
}
