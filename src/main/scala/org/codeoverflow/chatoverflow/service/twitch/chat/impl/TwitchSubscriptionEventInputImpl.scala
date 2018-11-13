package org.codeoverflow.chatoverflow.service.twitch.chat.impl

import org.codeoverflow.chatoverflow.api.io.input.event.TwitchSubscriptionEventInput
import org.codeoverflow.chatoverflow.service.Connection
import org.codeoverflow.chatoverflow.service.twitch.chat.TwitchChatConnector

class TwitchSubscriptionEventInputImpl extends Connection[TwitchChatConnector] with TwitchSubscriptionEventInput {
  override def init(): Unit = ???

  override def serialize(): String = ???

  override def deserialize(value: String): Unit = ???
}
