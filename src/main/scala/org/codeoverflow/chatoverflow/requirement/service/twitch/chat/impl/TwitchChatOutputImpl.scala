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

  override def sendChatMessage(message: String): Unit = sourceConnector.get.sendChatMessage(message)

  override def start(): Boolean = true
}
