package org.codeoverflow.chatoverflow.io.source

import org.codeoverflow.chatoverflow.io.connector.{ConnectorType, TwitchConnector}
import org.codeoverflow.chatoverflow.registry.ConnectorRegistry


trait TwitchSource extends Source {
  protected var twitchConnector: TwitchConnector = _

  override def
  setSource(sourceIdentifier: String): Unit = {
    ConnectorRegistry.getConnector(ConnectorType.Twitch, sourceIdentifier) match {
      case Some(connector: TwitchConnector) => twitchConnector = connector.asInstanceOf[TwitchConnector]
      case _ => throw new IllegalArgumentException("Connector not found.")
    }
  }

}
