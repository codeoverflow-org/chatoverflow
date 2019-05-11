package org.codeoverflow.chatoverflow.requirement.service.twitch.chat.impl

import org.codeoverflow.chatoverflow.api.io.input.event.TwitchSubscriptionEventInput
import org.codeoverflow.chatoverflow.requirement.Connection
import org.codeoverflow.chatoverflow.requirement.service.twitch.chat

class TwitchSubscriptionEventInputImpl extends Connection[chat.TwitchChatConnector] with TwitchSubscriptionEventInput {
  override def init(): Boolean = ???

  override def serialize(): String = ???

  override def deserialize(value: String): Unit = ???
}
