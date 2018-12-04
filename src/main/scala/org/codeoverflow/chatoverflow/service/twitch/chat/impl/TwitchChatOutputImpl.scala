package org.codeoverflow.chatoverflow.service.twitch.chat.impl


import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.api.io.output.chat.{ChatOutput, TwitchChatOutput}
import org.codeoverflow.chatoverflow.service.twitch.chat.TwitchChatConnector
import org.codeoverflow.chatoverflow.service.{Connection, Parent}

/**
  * This is the implementation of the twitch chat output, using the twitch connector.
  */
@Parent(classOf[ChatOutput])
class TwitchChatOutputImpl extends Connection[TwitchChatConnector] with TwitchChatOutput {

  private val logger = Logger.getLogger(this.getClass)

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
