package org.codeoverflow.chatoverflow.registry


import org.codeoverflow.chatoverflow.service.Connector

import scala.collection.mutable

object ConnectorRegistry {
  private val sources = mutable.Map[SourceKey, Connector]()

  def addConnector(connector: Connector): Unit = {
    val key = SourceKey(connector)
    if (!sources.contains(key)) {
      sources += key -> connector
    } else {
      throw new IllegalArgumentException(s"Source key '$key' does already exist!")
    }
  }

  def getConnector(connectorType: String, sourceIdentifier: String): Option[Connector] = {
    val key = SourceKey(connectorType, sourceIdentifier)
    sources.get(key)
  }

}

case class SourceKey(connectorType: String, sourceIdentifier: String)

object SourceKey {
  def apply(connector: Connector): SourceKey = SourceKey(connector.getUniqueTypeString, connector.sourceIdentifier)
}
