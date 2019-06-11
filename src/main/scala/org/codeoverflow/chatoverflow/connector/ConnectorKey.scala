package org.codeoverflow.chatoverflow.connector

/**
  * Connector keys are used to identify a unique connector instance.
  *
  * @param sourceIdentifier       the identifier for the platform source
  * @param qualifiedConnectorType the fully qualified type string of the connector
  */
case class ConnectorKey(sourceIdentifier: String, qualifiedConnectorType: String)
