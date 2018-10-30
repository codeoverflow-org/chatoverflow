package org.codeoverflow.chatoverflow.service.twitch.chat.impl

import java.util.Calendar
import java.util.function.Consumer

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.api.io.input.chat._
import org.codeoverflow.chatoverflow.service.Connection
import org.codeoverflow.chatoverflow.service.twitch.chat.TwitchChatConnector
import org.pircbotx.hooks.events.{MessageEvent, UnknownEvent}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/**
  * This is the implementation of the twitch chat input, using the twitch connector.
  */
class TwitchChatInputImpl extends Connection[TwitchChatConnector] with TwitchChatInput {

  private val logger = Logger.getLogger(this.getClass)

  private val messages: ListBuffer[TwitchChatMessage] = ListBuffer[TwitchChatMessage]()
  private val privateMessages: ListBuffer[TwitchChatMessage] = ListBuffer[TwitchChatMessage]()
  private val whisperRegex = """^:([^!]+?)!.*?:(.*)$""".r
  private val wholeEmoticonRegex = """(\d+):([\d,-]+)""".r
  private val emoticonRegex = """(\d+)-(\d+)""".r

  private val messageHandler = ListBuffer[Consumer[TwitchChatMessage]]()
  private val privateMessageHandler = ListBuffer[Consumer[TwitchChatMessage]]()

  override def init(): Unit = {

    // Add the own message handler to the twitch connector
    if (sourceConnector.isDefined) {
      sourceConnector.get.addMessageEventListener(onMessage)
      sourceConnector.get.addUnknownEventListener(onUnknown)
      sourceConnector.get.init()
    } else {
      logger warn "Source connector not set."
    }
  }

  private def onMessage(event: MessageEvent): Unit = {
    val message = event.getMessage
    val color = if (event.getV3Tags.get("color").contains("#")) event.getV3Tags.get("color") else ""
    val subscriber = event.getV3Tags.get("subscriber") == "1"
    val moderator = event.getV3Tags.get("mod") == "1"
    val broadcaster = event.getV3Tags.get("badges").contains("broadcaster/1")
    val premium = event.getV3Tags.get("badges").contains("premium/1")
    val author = new TwitchChatMessageAuthor(event.getUser.getNick, broadcaster, moderator, subscriber, premium)
    val channel = new Channel(event.getChannelSource)
    val emoticons = new java.util.ArrayList[ChatEmoticon]()
    wholeEmoticonRegex.findAllMatchIn(event.getV3Tags.get("emotes")).foreach(matchedElement => {
      val id = matchedElement.group(1)
      emoticonRegex.findAllMatchIn(matchedElement.group(2)).foreach(matchedElement => {
        val index = matchedElement.group(1).toInt
        val regex = message.substring(index, matchedElement.group(2).toInt + 1)
        val emoticon = new TwitchChatEmoticon(regex, id, index)
        emoticons.add(emoticon)
      })
    })
    val msg = new TwitchChatMessage(author, message, event.getTimestamp, channel, emoticons, color)

    messageHandler.foreach(consumer => consumer.accept(msg))
    messages += msg
  }

  private def onUnknown(event: UnknownEvent): Unit = {
    val matchedElement = whisperRegex.findFirstMatchIn(event.getLine)

    if (matchedElement.isDefined) {
      val name = matchedElement.get.group(1)
      val message = matchedElement.get.group(2)
      val timestamp = event.getTimestamp
      val msg = new TwitchChatMessage(new TwitchChatMessageAuthor(name), message, timestamp, null)

      privateMessageHandler.foreach(consumer => consumer.accept(msg))
      privateMessages += msg
    }
  }

  override def
  getLastMessages(lastMilliseconds: Long): java.util.List[TwitchChatMessage] = {
    val currentTime = Calendar.getInstance.getTimeInMillis

    messages.filter(_.getTimestamp > currentTime - lastMilliseconds).toList.asJava
  }


  override def getLastPrivateMessages(lastMilliseconds: Long): java.util.List[TwitchChatMessage] = {
    val currentTime = Calendar.getInstance.getTimeInMillis

    privateMessages.filter(_.getTimestamp > currentTime - lastMilliseconds).toList.asJava
  }

  override def registerMessageHandler(handler: Consumer[TwitchChatMessage]): Unit = messageHandler += handler

  override def registerPrivateMessageHandler(handler: Consumer[TwitchChatMessage]): Unit = privateMessageHandler += handler

  override def setChannel(channel: String): Unit = sourceConnector.get.setChannel(channel)
}
