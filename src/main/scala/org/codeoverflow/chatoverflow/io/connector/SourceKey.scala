package org.codeoverflow.chatoverflow.io.connector

import org.codeoverflow.chatoverflow.io.connector.ConnectorType.ConnectorType

case class SourceKey(connectorType: ConnectorType, sourceIdentifier: String) {
}

object SourceKey {
  def apply(connector: Connector): SourceKey = SourceKey(connector.getType, connector.sourceId)
}
