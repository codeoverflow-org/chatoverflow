package org.codeoverflow.chatoverflow.requirement.service.twitch.chat

import org.codeoverflow.chatoverflow.requirement.impl.EventManager
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.{MessageEvent, UnknownEvent}

/**
  * The twitch chat listener class holds the handlers to react to messages using the IRC bot.
  */
class TwitchChatListener extends ListenerAdapter with EventManager {

  override def onMessage(event: MessageEvent): Unit = {
    call(event)
  }

  override def onUnknown(event: UnknownEvent): Unit = {
    call(event)
  }
}
