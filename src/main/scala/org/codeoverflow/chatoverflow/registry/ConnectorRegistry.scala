package org.codeoverflow.chatoverflow.registry

import org.codeoverflow.chatoverflow.io.connector.ConnectorType.ConnectorType
import org.codeoverflow.chatoverflow.io.connector.{Connector, SourceKey}

import scala.collection.mutable

object ConnectorRegistry {
  private val sources = mutable.Map[SourceKey, Connector]()

  def addConnector(connector: Connector): Unit = {
    if (!sources.contains(SourceKey(connector))) {
      sources += SourceKey(connector.getType, connector.sourceId) -> connector
    } else {
      throw new IllegalArgumentException("Source key does already exist!")
    }
  }

  def getConnector(connectorType: ConnectorType, sourceId: String): Option[Connector] = {
    if (!sources.contains(SourceKey(connectorType, sourceId))) {
      None
    } else {
      Some(sources(SourceKey(connectorType, sourceId)))
    }
  }

}