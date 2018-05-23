package org.codeoverflow.chatoverflow.io.connector

import org.codeoverflow.chatoverflow.io.connector.ConnectorType.ConnectorType

class TwitchConnector(channelName: String) extends Connector(channelName) {
  override def getType: ConnectorType = TwitchConnector.getType
}

object TwitchConnector {
  def getType: ConnectorType = ConnectorType.Twitch
}
