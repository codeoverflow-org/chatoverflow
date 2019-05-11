package org.codeoverflow.chatoverflow.requirement.service.tipeeestream

import org.codeoverflow.chatoverflow.connector.Connector

class TipeeeStreamConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) {

  override protected var requiredCredentialKeys: List[String] = List()
  override protected var optionalCredentialKeys: List[String] = List()

  /**
    * Starts the connector, e.g. creates a connection with its platform.
    */
  override def start(): Boolean = ???

  /**
    * This stops the activity of the connector, e.g. by closing the platform connection.
    */
  override def stop(): Boolean = ???
}
