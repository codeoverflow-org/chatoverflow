package org.codeoverflow.chatoverflow.requirement.service.twitch.chat

import org.codeoverflow.chatoverflow.requirement.impl.EventManager
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.{MessageEvent, UnknownEvent}

/**
  * The twitch chat listener class holds the handlers to react to messages using the IRC bot.
  */
class TwitchChatListener extends ListenerAdapter with EventManager {

//  private val messageEventListener = ListBuffer[MessageEvent => Unit]()
//  private val unknownEventListener = ListBuffer[UnknownEvent => Unit]()

  override def onMessage(event: MessageEvent): Unit = {
//    messageEventListener.foreach(listener => listener(event))
    call(event)
  }

  override def onUnknown(event: UnknownEvent): Unit = {
    call(event)
//    unknownEventListener.foreach(listener => listener(event))
  }

/*  def addMessageEventListener(listener: MessageEvent => Unit): Unit = {
    messageEventListener += listener
  }

  def addUnknownEventListener(listener: UnknownEvent => Unit): Unit = {
    unknownEventListener += listener
  }

  def removeMessageEventListener(listener: MessageEvent => Unit): Unit = {
    messageEventListener -= listener
  }

  def removeUnknownEventListener(listener: UnknownEvent => Unit): Unit = {
    unknownEventListener -= listener
  }*/

}
