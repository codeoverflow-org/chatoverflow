package org.codeoverflow.chatoverflow2.service.tipeeestream

import org.codeoverflow.chatoverflow.configuration.Credentials
import org.codeoverflow.chatoverflow2.requirement.Connector

class TipeeeStreamConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) {
  /**
    * Returns true, if the connector has been already instantiated and is running.
    */
  override def isRunning: Boolean = ???

  /**
    * Shuts down the connector, closes its platform connection.
    */
  override def shutdown(): Unit = ???

  /**
    * Sets the credentials needed for login or authentication of the connector to its platform
    *
    * @param credentials the credentials object to login to the platform
    */
  override def setCredentials(credentials: Credentials): Unit = ???

  /**
    * Initializes the connector, e.g. creates a connection with its platform.
    */
  override def init(): Unit = ???
}
