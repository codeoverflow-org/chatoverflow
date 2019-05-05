package org.codeoverflow.chatoverflow.requirement.service.discord

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.{MessageDeleteEvent, MessageReceivedEvent, MessageUpdateEvent}
import net.dv8tion.jda.api.hooks.EventListener

import scala.collection.mutable.ListBuffer

/**
  * The discord chat listener class holds the handler to react to creation, edits and removal of messages using the rest api
  */
class DiscordChatListener extends EventListener {

  private val messageEventListener = ListBuffer[MessageReceivedEvent => Unit]()

  private val messageUpdateEventListener = ListBuffer[MessageUpdateEvent => Unit]()

  private val messageDeleteEventListener = ListBuffer[MessageDeleteEvent => Unit]()

  def addMessageEventListener(listener: MessageReceivedEvent => Unit): Unit = {
    messageEventListener += listener
  }

  def addMessageUpdateEventListener(listener: MessageUpdateEvent => Unit): Unit = {
    messageUpdateEventListener += listener
  }

  def addMessageDeleteEventListener(listener: MessageDeleteEvent => Unit): Unit = {
    messageDeleteEventListener += listener
  }

  override def onEvent(event: GenericEvent): Unit = {
    event match {
      case receivedEvent: MessageReceivedEvent => messageEventListener.foreach(listener => listener(receivedEvent))
      case updateEvent: MessageUpdateEvent => messageUpdateEventListener.foreach(listener => listener(updateEvent))
      case deleteEvent: MessageDeleteEvent => messageDeleteEventListener.foreach(listener => listener(deleteEvent))
      case _ => //Any other event, do nothing
    }
  }
}
