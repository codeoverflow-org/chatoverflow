package org.codeoverflow.chatoverflow.requirement.service.discord.impl

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.output.chat.DiscordChatOutput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.OutputImpl
import org.codeoverflow.chatoverflow.requirement.service.discord.DiscordChatConnector

/**
  * This is the implementation of the discord chat output, using the discord connector.
  */
@Impl(impl = classOf[DiscordChatOutput], connector = classOf[DiscordChatConnector])
class DiscordChatOutputImpl extends OutputImpl[DiscordChatConnector] with DiscordChatOutput with WithLogger {

  private var channelId: Option[String] = None

  override def start(): Boolean = true

  override def setChannel(channelId: String): Unit = {
    sourceConnector.get.getTextChannel(channelId) match {
      case Some(_) => this.channelId = Some(channelId.trim)
      case None => throw new IllegalArgumentException("Channel with that id doesn't exist")
    }
  }

  override def getChannelId: String = channelId.get

  override def sendChatMessage(message: String): Unit = {
    val channel = channelId.getOrElse(throw new IllegalStateException("first set the channel for this output"))
    sourceConnector.get.sendChatMessage(channel, message)
  }
}
