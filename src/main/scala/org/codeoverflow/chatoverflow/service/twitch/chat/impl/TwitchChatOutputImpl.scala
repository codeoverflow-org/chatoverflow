package org.codeoverflow.chatoverflow.service.twitch.chat.impl


import org.codeoverflow.chatoverflow.api.io.output.chat.TwitchChatOutput
import org.codeoverflow.chatoverflow.service.Connection
import org.codeoverflow.chatoverflow.service.twitch.chat.TwitchChatConnector

/**
  * This is the implementation of the twitch chat output, using the twitch connector.
  */
class TwitchChatOutputImpl extends Connection[TwitchChatConnector] with TwitchChatOutput {
  override def sendChatMessage(message: String): Unit = sourceConnector.sendChatMessage(message)

  override def init(): Unit = sourceConnector.init()
}
