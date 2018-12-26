package org.codeoverflow.chatoverflow2.connector

import org.codeoverflow.chatoverflow2.configuration.Credentials

/**
  * A connector is used to connect a input / output service to its dedicated platform
  *
  * @param sourceIdentifier the unique source identifier (e.g. a login name), the connector should work with
  */
abstract class Connector(val sourceIdentifier: String) {
  protected var credentials: Option[Credentials] = None
  protected var requiredCredentialKeys: List[String] = List[String]()

  /**
    * Returns the keys that should be set in the credentials object
    *
    * @return a list of keys
    */
  def getRequiredCredentialKeys: List[String] = requiredCredentialKeys

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
  def setCredentials(credentials: Credentials): Unit = this.credentials = Some(credentials)

  /**
    * Returns if the credentials had been set. Can be asked before running the init()-function.
    *
    * @return true if the credentials are not none. Does say nothing about their value quality
    */
  def areCredentialsSet: Boolean = credentials.isDefined

  /**
    * Initializes the connector, e.g. creates a connection with its platform.
    */
  def init(): Boolean

  /**
    * Shuts down the connector, closes its platform connection.
    */
  def shutdown(): Unit
}