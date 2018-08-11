package org.codeoverflow.chatoverflow.service.twitch.impl


import org.codeoverflow.chatoverflow.api.io.output.chat.TwitchChatOutput
import org.codeoverflow.chatoverflow.service.Connection
import org.codeoverflow.chatoverflow.service.twitch.TwitchConnector

/**
  * This is the implementation of the twitch chat output, using the twitch connector.
  */
class TwitchChatOutputImpl extends Connection[TwitchConnector] with TwitchChatOutput {
  override def sendChatMessage(message: String): Unit = sourceConnector.sendChatMessage("", message)

  override def init(): Unit = sourceConnector.init()
}
