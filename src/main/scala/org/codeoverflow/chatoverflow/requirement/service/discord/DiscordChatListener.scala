package org.codeoverflow.chatoverflow.requirement.service.discord

import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.react.{MessageReactionAddEvent, MessageReactionRemoveEvent}
import net.dv8tion.jda.core.events.message.{MessageDeleteEvent, MessageReceivedEvent, MessageUpdateEvent}
import net.dv8tion.jda.core.hooks.EventListener

import scala.collection.mutable.ListBuffer

/**
  * The discord chat listener class holds the handler to react to creation, edits and removal of messages using the rest api
  */
class DiscordChatListener extends EventListener {

  private val messageEventListener = ListBuffer[MessageReceivedEvent => Unit]()

  private val messageUpdateEventListener = ListBuffer[MessageUpdateEvent => Unit]()

  private val messageDeleteEventListener = ListBuffer[MessageDeleteEvent => Unit]()

  private val reactionAddEventListener = ListBuffer[MessageReactionAddEvent => Unit]()

  private val reactionDelEventListener = ListBuffer[MessageReactionRemoveEvent => Unit]()

  def addMessageReceivedListener(listener: MessageReceivedEvent => Unit): Unit = messageEventListener += listener

  def addMessageUpdateEventListener(listener: MessageUpdateEvent => Unit): Unit = messageUpdateEventListener += listener

  def addMessageDeleteEventListener(listener: MessageDeleteEvent => Unit): Unit = messageDeleteEventListener += listener

  def addReactionAddEventListener(listener: MessageReactionAddEvent => Unit): Unit = reactionAddEventListener += listener

  def addReactionDelEventListener(listener: MessageReactionRemoveEvent => Unit): Unit = reactionDelEventListener += listener

  def removeMessageReceivedListener(listener: MessageReceivedEvent => Unit): Unit = messageEventListener -= listener

  def removeMessageUpdateEventListener(listener: MessageUpdateEvent => Unit): Unit = messageUpdateEventListener -= listener

  def removeMessageDeleteEventListener(listener: MessageDeleteEvent => Unit): Unit = messageDeleteEventListener -= listener

  def removeReactionAddEventListener(listener: MessageReactionAddEvent => Unit): Unit = reactionAddEventListener -= listener

  def removeReactionDelEventListener(listener: MessageReactionRemoveEvent => Unit): Unit = reactionDelEventListener -= listener

  override def onEvent(event: Event): Unit = {
    event match {
      case receivedEvent: MessageReceivedEvent => messageEventListener.foreach(listener => listener(receivedEvent))
      case updateEvent: MessageUpdateEvent => messageUpdateEventListener.foreach(listener => listener(updateEvent))
      case deleteEvent: MessageDeleteEvent => messageDeleteEventListener.foreach(listener => listener(deleteEvent))
      case reactionAddEvent: MessageReactionAddEvent => reactionAddEventListener.foreach(listener => listener(reactionAddEvent))
      case reactionDelEvent: MessageReactionRemoveEvent => reactionDelEventListener.foreach(listener => listener(reactionDelEvent))
      case _ => //Any other event, do nothing
    }
  }
}
