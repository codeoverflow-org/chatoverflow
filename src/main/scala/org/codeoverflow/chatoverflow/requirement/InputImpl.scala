package org.codeoverflow.chatoverflow.requirement

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.input.Input
import org.codeoverflow.chatoverflow.connector.Connector

import scala.reflect.ClassTag

abstract class InputImpl[C <: Connector](implicit ct: ClassTag[C]) extends Connection[C] with Input with WithLogger {

  /**
    * Inits this connection, checks if teh source connector is defined, and can be inited, then calls start
    *
    * @return if this input could be successfully inited
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
    * Start the input, called after source connector did init
    *
    * @return true if starting the input was successful, false if some problems occurred
    */
  def start(): Boolean


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
