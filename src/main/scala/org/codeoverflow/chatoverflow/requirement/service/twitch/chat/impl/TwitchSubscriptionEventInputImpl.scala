package org.codeoverflow.chatoverflow.requirement.service.twitch.chat.impl

import org.codeoverflow.chatoverflow.api.io.input.event.TwitchSubscriptionEventInput
import org.codeoverflow.chatoverflow.requirement.InputImpl
import org.codeoverflow.chatoverflow.requirement.service.twitch.chat

class TwitchSubscriptionEventInputImpl extends InputImpl[chat.TwitchChatConnector] with TwitchSubscriptionEventInput {

  override def serialize(): String = ???

  override def deserialize(value: String): Unit = ???

  /**
    * Start the input, called after source connector did init
    *
    * @return true if starting the input was successful, false if some problems occurred
    */
  override def start(): Boolean = ???

  /**
    * Stops the input, called before source connector will shutdown
    *
    * @return true if stopping was successful
    */
  override def stop(): Boolean = ???
}
