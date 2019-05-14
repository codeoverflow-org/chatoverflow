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

  private var channelId: String = _


  override def setChannel(channelId: String): Unit = {
    sourceConnector.get.getTextChannel(channelId) match {
      case Some(_) => this.channelId = channelId
      case None => throw new IllegalArgumentException("Channel with that id doesn't exist")
    }
  }

  override def getChannelId: String = channelId

  override def init(): Boolean = {
    if (sourceConnector.isDefined) {
      if (sourceConnector.get.isRunning || sourceConnector.get.init()) {
        setChannel(getSourceIdentifier)
        true
      } else false
    } else {
      logger warn "Source Connector not set."
      false
    }
  }

  override def sendChatMessage(message: String): Unit = sourceConnector.get.sendChatMessage(channelId, message)

  override def serialize(): String = getSourceIdentifier

  override def deserialize(value: String): Unit = {
    setSourceConnector(value)
  }
}
