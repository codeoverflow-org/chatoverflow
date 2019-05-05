package org.codeoverflow.chatoverflow.requirement.service.discord.impl

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.output.chat.DiscordChatOutput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.Connection
import org.codeoverflow.chatoverflow.requirement.service.discord.DiscordChatConnector

/**
  * This is the implementation of the discord chat output, using the discord connector.
  */
@Impl(impl = classOf[DiscordChatOutput], connector = classOf[DiscordChatConnector])
class DiscordChatOutputImpl extends Connection[DiscordChatConnector] with DiscordChatOutput with WithLogger {

  override def sendChatMessage(message: String): Unit = sourceConnector.get.sendChatMessage(message)

  override def init(): Unit = {
    if (sourceConnector.isDefined) {
      sourceConnector.get.init()
    } else {
      logger warn "Source Connector not set."
    }
  }

  override def serialize(): String = getSourceIdentifier

  override def deserialize(value: String): Unit = {
    setSourceConnector(value)
  }
}
