package org.codeoverflow.chatoverflow.service.twitch.chat.impl


import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.api.io.output.chat.TwitchChatOutput
import org.codeoverflow.chatoverflow.service.Connection
import org.codeoverflow.chatoverflow.service.twitch.chat.TwitchChatConnector

/**
  * This is the implementation of the twitch chat output, using the twitch connector.
  */
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
}
