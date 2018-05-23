package org.codeoverflow.chatoverflow.io.source

import org.codeoverflow.chatoverflow.api.io.connector.TwitchConnector


trait TwitchSource extends Source {
  protected var twitchConnector: TwitchConnector = _

  override def setSource(sourceIdentifier: String): Unit = {
    //twitchConnector = Registry.getConnector(ConnectorSource.TWITCH, sourceIdentifier)
  }

}
