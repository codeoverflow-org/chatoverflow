package org.codeoverflow.chatoverflow.requirement.service.twitch.chat

import org.pircbotx.hooks.events.{ConnectAttemptFailedEvent, ConnectEvent, NoticeEvent}
import org.pircbotx.hooks.{Event, ListenerAdapter}

/**
  * Handles connection events for the TwitchChatConnector.
  * Calls the callback function once the bot connected and reports connection errors.
  * @param fn the callback which will be called once suitable event has been received.
  *           The first param informs whether the connection could be established successfully
  *           and the second param includes a error description if something has gone wrong.
  */
class TwitchChatConnectListener(fn: (Boolean, String) => Unit) extends ListenerAdapter {
  override def onEvent(event: Event): Unit = {
    event match {
      case _: ConnectEvent => fn(true, "")
      case e: ConnectAttemptFailedEvent => fn(false, "couldn't connect to irc chat server")
      case e: NoticeEvent =>
        if (e.getNotice.contains("authentication failed")) {
          fn(false, "authentication failed")
        }
      case _ =>
    }
  }
}
