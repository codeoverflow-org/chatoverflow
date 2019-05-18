package org.codeoverflow.chatoverflow.requirement.service.twitch.chat.impl

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.output.chat.TwitchChatOutput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.OutputImpl
import org.codeoverflow.chatoverflow.requirement.service.twitch.chat

/**
  * This is the implementation of the twitch chat output, using the twitch connector.
  */
@Impl(impl = classOf[TwitchChatOutput], connector = classOf[chat.TwitchChatConnector])
class TwitchChatOutputImpl extends OutputImpl[chat.TwitchChatConnector] with TwitchChatOutput with WithLogger {

  private var currentChannel: Option[String] = None

  override def sendChatMessage(message: String): Unit = {
    currentChannel match {
      case Some(value) => sourceConnector.get.sendChatMessage(value, message)
      case None => throw new IllegalStateException("first set the channel for this output")
    }
  }

  override def start(): Boolean = true

  override def setChannel(channel: String): Unit = {
    currentChannel = Some(channel.trim)
    if (!sourceConnector.get.isJoined(channel.trim)) sourceConnector.get.joinChannel(channel.trim)
  }
}
