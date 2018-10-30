package org.codeoverflow.chatoverflow.service.discord

import org.codeoverflow.chatoverflow.configuration.Credentials
import org.codeoverflow.chatoverflow.service.Connector

class DiscordChatConnector(override val sourceIdentifier: String, credentials: Credentials) extends Connector(sourceIdentifier, credentials) {
  override def init(): Unit = {

  }

  override def isRunning: Boolean = true

  override def shutdown(): Unit = {

  }

  override def getUniqueTypeString: String = this.getClass.getName
}
