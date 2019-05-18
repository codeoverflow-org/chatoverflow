package org.codeoverflow.chatoverflow.requirement.service.twitch.chat.impl

import java.util.Calendar
import java.util.function.Consumer

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.dto.chat.twitch.{TwitchChatEmoticon, TwitchChatMessage, TwitchChatMessageAuthor}
import org.codeoverflow.chatoverflow.api.io.dto.chat.{Channel, ChatEmoticon}
import org.codeoverflow.chatoverflow.api.io.input.chat._
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.InputImpl
import org.codeoverflow.chatoverflow.requirement.service.twitch.chat
import org.pircbotx.hooks.events.{MessageEvent, UnknownEvent}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/**
  * This is the implementation of the twitch chat input, using the twitch connector.
  */
@Impl(impl = classOf[TwitchChatInput], connector = classOf[chat.TwitchChatConnector])
class TwitchChatInputImpl extends InputImpl[chat.TwitchChatConnector] with TwitchChatInput with WithLogger {

  private val messages: ListBuffer[TwitchChatMessage] = ListBuffer[TwitchChatMessage]()
  private val privateMessages: ListBuffer[TwitchChatMessage] = ListBuffer[TwitchChatMessage]()
  private val whisperRegex = """^:([^!]+?)!.*?:(.*)$""".r
  private val wholeEmoticonRegex = """(\d+):([\d,-]+)""".r
  private val emoticonRegex = """(\d+)-(\d+)""".r

  private val messageHandler = ListBuffer[Consumer[TwitchChatMessage]]()
  private val privateMessageHandler = ListBuffer[Consumer[TwitchChatMessage]]()

  private var currentChannel: Option[String] = None

  override def start(): Boolean = {
    sourceConnector.get.addMessageEventListener(onMessage)
    sourceConnector.get.addUnknownEventListener(onUnknown)
    true
  }

  private def onMessage(event: MessageEvent): Unit = {
    if (currentChannel.isDefined && event.getChannelSource == currentChannel.get) {
      val message = event.getMessage
      val color = if (event.getV3Tags.get("color").contains("#")) event.getV3Tags.get("color") else ""
      val subscriber = event.getV3Tags.get("subscriber") == "1"
      val moderator = event.getV3Tags.get("mod") == "1"
      val broadcaster = event.getV3Tags.get("badges").contains("broadcaster/1")
      val turbo = event.getV3Tags.get("badges").contains("turbo/1")
      val author = new TwitchChatMessageAuthor(event.getUser.getNick, color, broadcaster, moderator, subscriber, turbo)
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
      val msg = new TwitchChatMessage(author, message, event.getTimestamp, channel, emoticons)

      messageHandler.foreach(consumer => consumer.accept(msg))
      messages += msg
    }
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

  override def getLastMessages(lastMilliseconds: Long): java.util.List[TwitchChatMessage] = {
    if (currentChannel.isEmpty) throw new IllegalStateException("first set the channel for this input")
    val currentTime = Calendar.getInstance.getTimeInMillis

    messages.filter(_.getTimestamp > currentTime - lastMilliseconds).toList.asJava
  }


  override def getLastPrivateMessages(lastMilliseconds: Long): java.util.List[TwitchChatMessage] = {
    val currentTime = Calendar.getInstance.getTimeInMillis

    privateMessages.filter(_.getTimestamp > currentTime - lastMilliseconds).toList.asJava
  }

  override def registerMessageHandler(handler: Consumer[TwitchChatMessage]): Unit = {
    if (currentChannel.isEmpty) throw new IllegalStateException("first set the channel for this input")
    messageHandler += handler
  }

  override def registerPrivateMessageHandler(handler: Consumer[TwitchChatMessage]): Unit = privateMessageHandler += handler

  override def setChannel(channel: String): Unit = {
    currentChannel = Some(channel.trim)
    if (!sourceConnector.get.isJoined(channel.trim)) sourceConnector.get.joinChannel(channel.trim)
  }
}