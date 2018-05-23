package org.codeoverflow.chatoverflow.io.output.chat

import org.codeoverflow.chatoverflow.api.io.output.chat.TwitchChatOutput
import org.codeoverflow.chatoverflow.io.source.TwitchSource

class TwitchChatOutputImpl extends TwitchSource with TwitchChatOutput {
  override def sendChatMessage(message: String): Unit = twitchConnector.sendChatMessage(message)
}
