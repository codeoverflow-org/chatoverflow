package org.codeoverflow.chatoverflow.service.twitch

import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.{MessageEvent, UnknownEvent}

import scala.collection.mutable.ListBuffer

/**
  * The twitch chat listener class holds the handlers to react to messages using the IRC bot.
  */
class TwitchChatListener extends ListenerAdapter {

  private val messageEventListener = ListBuffer[MessageEvent => Unit]()
  private val unknownEventListener = ListBuffer[UnknownEvent => Unit]()

  override def onMessage(event: MessageEvent): Unit = {
    messageEventListener.foreach(listener => listener(event))
  }

  override def onUnknown(event: UnknownEvent): Unit = {
    unknownEventListener.foreach(listener => listener(event))
  }

  def addMessageEventListener(listener: MessageEvent => Unit): Unit = {
    messageEventListener += listener
  }

  def addUnknownEventListener(listener: UnknownEvent => Unit): Unit = {
    unknownEventListener += listener
  }

}
