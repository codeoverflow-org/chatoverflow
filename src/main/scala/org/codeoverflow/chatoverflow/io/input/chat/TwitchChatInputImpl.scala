package org.codeoverflow.chatoverflow.io.input.chat

import java.util.Calendar
import java.util.function.Consumer

import org.codeoverflow.chatoverflow.api.io.input.chat.{ChatMessage, TwitchChatInput}
import org.codeoverflow.chatoverflow.io.source.TwitchSource
import org.pircbotx.hooks.events.{MessageEvent, UnknownEvent}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class TwitchChatInputImpl extends TwitchSource with TwitchChatInput {

  private val messages: ListBuffer[ChatMessage] = ListBuffer[ChatMessage]()
  private val privateMessages: ListBuffer[ChatMessage] = ListBuffer[ChatMessage]()
  private val whisperRegex = """^:([^!]+?)!.*?:(.*)$""".r

  private val messageHandler = ListBuffer[Consumer[ChatMessage]]()
  private val privateMessageHandler = ListBuffer[Consumer[ChatMessage]]()

  override def init(): Unit = {
    twitchConnector.addMessageEventListener(onMessage)
    twitchConnector.addUnknownEventListener(onUnknown)
  }

  def onMessage(event: MessageEvent): Unit = {
    val color = if (event.getV3Tags.get("color").contains("#")) event.getV3Tags.get("color") else ""
    val isSub = if (event.getV3Tags.get("subscriber") == "1") true else false
    val msg: ChatMessage = new ChatMessage(event.getMessage, event.getUser.getNick, event.getTimestamp, isSub, color)

    messageHandler.foreach(consumer => consumer.accept(msg))
    messages += msg
  }

  def onUnknown(event: UnknownEvent): Unit = {
    val matchedElement = whisperRegex.findFirstMatchIn(event.getLine)

    if (matchedElement.isDefined) {
      val name = matchedElement.get.group(1)
      val message = matchedElement.get.group(2)
      val timestamp = event.getTimestamp
      val msg: ChatMessage = new ChatMessage(message, name, timestamp)

      privateMessageHandler.foreach(consumer => consumer.accept(msg))
      privateMessages += msg
    }
  }

  override def
  getLastMessages(lastMilliseconds: Long): java.util.List[ChatMessage] = {
    val currentTime = Calendar.getInstance.getTimeInMillis

    messages.filter(_.getTimestamp > currentTime - lastMilliseconds).toList.asJava
  }


  override def getLastPrivateMessages(lastMilliseconds: Long): java.util.List[ChatMessage] = {
    val currentTime = Calendar.getInstance.getTimeInMillis

    privateMessages.filter(_.getTimestamp > currentTime - lastMilliseconds).toList.asJava
  }

  override def registerMessageHandler(handler: Consumer[ChatMessage]): Unit = messageHandler += handler

  override def registerPrivateMessageHandler(handler: Consumer[ChatMessage]): Unit = privateMessageHandler += handler
}
