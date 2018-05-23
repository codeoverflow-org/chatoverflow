package org.codeoverflow.chatoverflow.io.connector

import org.codeoverflow.chatoverflow.io.connector.ConnectorType.ConnectorType

abstract class Connector(val sourceId: String) {

  def getType: ConnectorType
}

object ConnectorType extends Enumeration {
  type ConnectorType = Value
  val Twitch, YouTube, TipeeeStream, Discord, MockUp = Value
}