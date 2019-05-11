package org.codeoverflow.chatoverflow.connector

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.configuration.Credentials

/**
  * A connector is used to connect a input / output service to its dedicated platform
  *
  * @param sourceIdentifier the unique source identifier (e.g. a login name), the connector should work with
  */
abstract class Connector(val sourceIdentifier: String) extends WithLogger {
  private val connectorSourceAndType = s"connector '§$sourceIdentifier' of type '$getUniqueTypeString'"
  protected var credentials: Option[Credentials] = None
  protected var requiredCredentialKeys: List[String]
  protected var running = false

  def getCredentials: Option[Credentials] = this.credentials

  /**
    * Sets the credentials needed for login or authentication of the connector to its platform
    *
    * @param credentials the credentials object to login to the platform
    */
  def setCredentials(credentials: Credentials): Unit = this.credentials = Some(credentials)

  def setCredentialsValue(key: String, value: String): Boolean = {
    if (credentials.isEmpty) false else {
      if (credentials.get.exists(key)) {
        credentials.get.removeValue(key)
      }
      credentials.get.addValue(key, value)
      true
    }
  }

  /**
    * Returns the keys that should be set in the credentials object
    *
    * @return a list of keys
    */
  def getRequiredCredentialKeys: List[String] = requiredCredentialKeys

  /**
    * Returns true, if the connector has been already instantiated and is running.
    */
  def isRunning: Boolean = running

  /**
    * Initializes the connector by checking the conditions and then calling the start method.
    */
  def init(): Boolean = {
    if (running) {
      logger warn s"Unable to start $connectorSourceAndType. Already running!"
      false
    } else {
      if (!areCredentialsSet) {
        logger warn s"Unable to start $connectorSourceAndType. Not all credentials are set."
        val unsetCredentials = for (key <- requiredCredentialKeys if !credentials.get.exists(key)) yield key
        logger info s"Not set credentials are: ${unsetCredentials.mkString(", ")}."
        false
      } else {
        if (start()) {
          logger info s"Started $connectorSourceAndType."
          true
        } else {
          logger warn s"Failed starting $connectorSourceAndType."
          false
        }
      }
    }
  }

  /**
    * Returns if the credentials had been set. Can be asked before running the init()-function.
    *
    * @return true if the credentials are not none. Does say nothing about their value quality
    */
  def areCredentialsSet: Boolean = credentials.isDefined

  /**
    * Starts the connector, e.g. creates a connection with its platform.
    */
  def start(): Boolean

  /**
    * Shuts down the connector by calling the stop method.
    */
  def shutdown(): Unit = {
    if (stop()) {
      running = false
      logger info s"Stopped $connectorSourceAndType."
    } else {
      logger warn s"Unable to shutdown $connectorSourceAndType."
    }
  }

  /**
    * Returns the unique type string of the implemented connector.
    *
    * @return the class type
    */
  def getUniqueTypeString: String = this.getClass.getName

  /**
    * This stops the activity of the connector, e.g. by closing the platform connection.
    */
  def stop(): Boolean
}