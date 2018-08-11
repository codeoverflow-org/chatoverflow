package org.codeoverflow.chatoverflow.service

import org.codeoverflow.chatoverflow.registry.ConnectorRegistry

import scala.reflect.ClassTag

/**
  * A connection is used to encapsulate a connector when creating a input / output service.
  * The type is NOT erased using ClassTag and later used to retrieve the correct connector from the ConnectorRegistry.
  *
  * @param ct this is just scala magic, used to prevent generic type erasure at compile time
  * @tparam T the type of the connector, needed by the input / output service
  */
abstract class Connection[T <: Connector](implicit ct: ClassTag[T]) {
  private val connectorType: String = ct.runtimeClass.getName
  /**
    * This connector variable can be used to work with the platform specific connection
    */
  protected var sourceConnector: T = _
  private var sourceIdentifier: String = _

  /**
    * Sets the source connector by retrieving it from the registry, using the identifier
    *
    * @param sourceIdentifier a identifier of a previously registered connector
    */
  def setSourceConnector(sourceIdentifier: String): Unit = {
    this.sourceIdentifier = sourceIdentifier
    ConnectorRegistry.getConnector(connectorType, sourceIdentifier) match {
      case Some(connector: Connector) => sourceConnector = connector.asInstanceOf[T]
      case _ => throw new IllegalArgumentException("Connector not found.")
    }
  }

  /**
    * Returns the source identifier.
    *
    * @return a unique identifier of the connection and connector
    */
  def getSourceIdentifier: String = sourceIdentifier

}