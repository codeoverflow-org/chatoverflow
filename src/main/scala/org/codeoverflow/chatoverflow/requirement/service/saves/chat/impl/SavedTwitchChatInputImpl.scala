package org.codeoverflow.chatoverflow.requirement.service.saves.chat.impl

import org.codeoverflow.chatoverflow.api.io.input.chat.{TwitchChatInput, TwitchChatMessage}
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.service.saves.chat.SavedTwitchChatConnector

//@Impl(impl = classOf[TwitchChatInput], connector = classOf[SavedTwitchChatConnector])
class SavedTwitchChatInputImpl extends SavedChatInputImpl[TwitchChatMessage] with TwitchChatInput{
  override def setChannel(channel: String): Unit = {
    // TODO: maybe not ignore (?)
  }
}
