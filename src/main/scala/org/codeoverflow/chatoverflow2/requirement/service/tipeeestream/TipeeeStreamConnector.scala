package org.codeoverflow.chatoverflow2.requirement.service.tipeeestream

import org.codeoverflow.chatoverflow2.connector.Connector

class TipeeeStreamConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) {
  requiredCredentialKeys = List("", "", "")
  /**
    * Returns true, if the connector has been already instantiated and is running.
    */
  override def isRunning: Boolean = ???

  /**
    * Initializes the connector, e.g. creates a connection with its platform.
    */
  override def init(): Boolean = ???

  /**
    * Shuts down the connector, closes its platform connection.
    */
  override def shutdown(): Unit = ???
}
