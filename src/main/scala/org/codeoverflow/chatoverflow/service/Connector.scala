package org.codeoverflow.chatoverflow.service

import org.codeoverflow.chatoverflow.configuration.Credentials

/**
  * A connector is used to connect a input / output service to its dedicated platform
  *
  * @param sourceIdentifier the unique source identifier (e.g. a login name), the connector should work with
  * @param credentials      the credentials object to login to the platform
  */
abstract class Connector(val sourceIdentifier: String, credentials: Credentials) {

  /**
    * Returns the unique type string of the implemented connector.
    *
    * @return the class type, just use this.getClass.getName
    */
  def getUniqueTypeString: String

  /**
    * Returns true, if the connector has been already instantiated and is running.
    */
  def isRunning: Boolean

  /**
    * Initializes the connector, e.g. creates a connection with its platform.
    */
  def init(): Unit

  /**
    * Shuts down the connector, closes its platform connection.
    */
  def shutdown(): Unit
}