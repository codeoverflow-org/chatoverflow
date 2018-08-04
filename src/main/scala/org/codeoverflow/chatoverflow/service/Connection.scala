package org.codeoverflow.chatoverflow.service

import org.codeoverflow.chatoverflow.registry.ConnectorRegistry

import scala.reflect.ClassTag

abstract class Connection[T <: Connector](implicit ct: ClassTag[T]) {
  private val connectorType: String = ct.runtimeClass.getName
  private var sourceIdentifier: String = _
  protected var sourceConnector: T = _

  def setSourceConnector(sourceIdentifier: String): Unit = {
    this.sourceIdentifier = sourceIdentifier
    ConnectorRegistry.getConnector(connectorType, sourceIdentifier) match {
      case Some(connector: Connector) => sourceConnector = connector.asInstanceOf[T]
      case _ => throw new IllegalArgumentException("Connector not found.")
    }
  }

  def getSourceIdentifier: String = sourceIdentifier

}