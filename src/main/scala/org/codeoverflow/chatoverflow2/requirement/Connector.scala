package org.codeoverflow.chatoverflow2.requirement

import org.codeoverflow.chatoverflow.configuration.Credentials

/**
  * A connector is used to connect a input / output service to its dedicated platform
  *
  * @param sourceIdentifier the unique source identifier (e.g. a login name), the connector should work with
  */
abstract class Connector(val sourceIdentifier: String) {

  /**
    * Returns the unique type string of the implemented connector.
    *
    * @return the class type
    */
  def getUniqueTypeString: String = this.getClass.getName

  /**
    * Returns true, if the connector has been already instantiated and is running.
    */
  def isRunning: Boolean

  /**
    * Sets the credentials needed for login or authentication of the connector to its platform
    *
    * @param credentials the credentials object to login to the platform
    */
  def setCredentials(credentials: Credentials): Unit

  /**
    * Initializes the connector, e.g. creates a connection with its platform.
    */
  def init(): Unit

  /**
    * Shuts down the connector, closes its platform connection.
    */
  def shutdown(): Unit
}