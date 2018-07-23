package org.codeoverflow.chatoverflow.service.twitch

//import org.codeoverflow.chatoverflow.registry.ConnectorRegistry
//import org.codeoverflow.chatoverflow.service.{ConnectorType, Source}

/*
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
*/