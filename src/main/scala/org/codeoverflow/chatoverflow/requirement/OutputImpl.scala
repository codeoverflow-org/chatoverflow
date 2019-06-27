package org.codeoverflow.chatoverflow.requirement

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.output.Output
import org.codeoverflow.chatoverflow.connector.Connector

import scala.reflect.ClassTag

abstract class OutputImpl[C <: Connector](implicit ct: ClassTag[C]) extends Connection[C] with Output with WithLogger {

  /**
    * Init this connection, checks if teh source connector is defined, and can be inited, then calls start
    *
    * @return if this input could be successfully initialized
    */
  override def init(): Boolean = {
    if (sourceConnector.isDefined) {
      if (sourceConnector.get.init()) {
        start()
      } else false
    } else {
      logger warn "Source connector not set."
      false
    }
  }

  /**
    * Shuts down the connection by calling stop on the output and requesting shutdown on the connector.
    *
    * @return true if both parties tried to shut down
    */
  override def shutdown(): Boolean = {
    if (sourceConnector.isDefined) {
      stop() & sourceConnector.get.shutdown()
    } else {
      logger warn "Source connector not set."
      false
    }
  }

  /**
    * Start the input, called after source connector did init
    *
    * @return true if starting the input was successful, false if some problems occurred
    */
  def start(): Boolean

  /**
    * Stops the output, called before source connector will shutdown
    *
    * @return true if stopping was successful
    */
  def stop(): Boolean


  /**
    * Serializes this object into a string to save it to a config
    *
    * @return serialized
    */
  override def serialize(): String = getSourceIdentifier

  /**
    * Deserialize a string to apply provided config settings to this object
    *
    * @param value should be serialized
    */
  override def deserialize(value: String): Unit = setSourceConnector(value)
}
