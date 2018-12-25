package org.codeoverflow.chatoverflow2.service.twitch.chat.impl

import org.codeoverflow.chatoverflow.api.io.output.chat.TwitchChatOutput
import org.codeoverflow.chatoverflow2.WithLogger
import org.codeoverflow.chatoverflow2.requirement.{Connection, Impl}
import org.codeoverflow.chatoverflow2.service.twitch.chat

/**
  * This is the implementation of the twitch chat output, using the twitch connector.
  */
@Impl(classOf[TwitchChatOutput])
class TwitchChatOutputImpl extends Connection[chat.TwitchChatConnector] with TwitchChatOutput with WithLogger {

  override def sendChatMessage(message: String): Unit = sourceConnector.get.sendChatMessage(message)

  override def init(): Unit = {
    if (sourceConnector.isDefined) {
      sourceConnector.get.init()
    } else {
      logger warn "Source Connector not set."
    }
  }

  override def serialize(): String = ???

  override def deserialize(value: String): Unit = ???
}
