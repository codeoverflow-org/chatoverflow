package org.codeoverflow.chatoverflow2.service.twitch.chat.impl

import org.codeoverflow.chatoverflow.api.io.input.event.TwitchSubscriptionEventInput
import org.codeoverflow.chatoverflow2.requirement.Connection
import org.codeoverflow.chatoverflow2.service.twitch.chat

class TwitchSubscriptionEventInputImpl extends Connection[chat.TwitchChatConnector] with TwitchSubscriptionEventInput {
  override def init(): Unit = ???

  override def serialize(): String = ???

  override def deserialize(value: String): Unit = ???
}