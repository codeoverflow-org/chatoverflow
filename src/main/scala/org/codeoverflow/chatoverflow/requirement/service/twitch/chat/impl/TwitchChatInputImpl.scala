package org.codeoverflow.chatoverflow.requirement.service.twitch.chat.impl

import java.time.temporal.ChronoUnit
import java.time.{Instant, OffsetDateTime, ZoneOffset}

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.dto.chat.twitch.{TwitchChatEmoticon, TwitchChatMessage, TwitchChatMessageAuthor}
import org.codeoverflow.chatoverflow.api.io.dto.chat.{ChatEmoticon, TextChannel}
import org.codeoverflow.chatoverflow.api.io.event.chat.twitch.{TwitchChatMessageReceiveEvent, TwitchEvent, TwitchPrivateChatMessageReceiveEvent}
import org.codeoverflow.chatoverflow.api.io.input.chat._
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.impl.EventInputImpl
import org.codeoverflow.chatoverflow.requirement.service.twitch.chat
import org.codeoverflow.chatoverflow.requirement.service.twitch.chat.TwitchChatConnector
import org.pircbotx.hooks.events.{MessageEvent, UnknownEvent}

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._

/**
  * This is the implementation of the twitch chat input, using the twitch connector.
  */
@Impl(impl = classOf[TwitchChatInput], connector = classOf[chat.TwitchChatConnector])
class TwitchChatInputImpl extends EventInputImpl[TwitchEvent, chat.TwitchChatConnector] with TwitchChatInput with WithLogger {

  private val messages: ListBuffer[TwitchChatMessage] = ListBuffer[TwitchChatMessage]()
  private val privateMessages: ListBuffer[TwitchChatMessage] = ListBuffer[TwitchChatMessage]()
  private val whisperRegex = """^:([^!]+?)!.*?:(.*)$""".r
  private val wholeEmoticonRegex = """(\d+):([\d,-]+)""".r
  private val emoticonRegex = """(\d+)-(\d+)""".r

  private var currentChannel: Option[String] = None
  private var ignoreOwnMessages = false

  override def start(): Boolean = {
    sourceConnector.get.registerEventHandler(onMessage _)
    sourceConnector.get.registerEventHandler(onUnknown _)
    true
  }

  private def onMessage(event: MessageEvent): Unit = {
    if (currentChannel.isDefined && event.getChannelSource == currentChannel.get
      && (!ignoreOwnMessages || event.getUser.getNick.toLowerCase != getUsername)) {

      val message = event.getMessage
      val color = if (event.getV3Tags.get("color").contains("#")) event.getV3Tags.get("color") else ""
      val subscriber = event.getV3Tags.get("subscriber") == "1"
      val moderator = event.getV3Tags.get("mod") == "1"
      val broadcaster = event.getV3Tags.get("badges").contains("broadcaster/1")
      val turbo = event.getV3Tags.get("badges").contains("turbo/1")
      val vip = event.getV3Tags.get("badges").contains("vip/1")
      val author = new TwitchChatMessageAuthor(event.getUser.getNick, color, broadcaster, moderator, subscriber, turbo, vip)
      val time = OffsetDateTime.ofInstant(Instant.ofEpochMilli(event.getTimestamp), ZoneOffset.UTC)
      val channel = new TextChannel(event.getChannelSource)
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
      val msg = new TwitchChatMessage(author, message, time, channel, emoticons)


      messages += msg
      call(new TwitchChatMessageReceiveEvent(msg))
    }
  }

  private def onUnknown(event: UnknownEvent): Unit = {
    val matchedElement = whisperRegex.findFirstMatchIn(event.getLine)

    if (matchedElement.isDefined) {
      val name = matchedElement.get.group(1)
      val message = matchedElement.get.group(2)
      val time = OffsetDateTime.ofInstant(Instant.ofEpochMilli(event.getTimestamp), ZoneOffset.UTC)
      val msg = new TwitchChatMessage(new TwitchChatMessageAuthor(name), message, time, null)

      privateMessages += msg
      call(new TwitchPrivateChatMessageReceiveEvent(msg))
    }
  }

  override def getLastMessages(lastMilliseconds: Long): java.util.List[TwitchChatMessage] = {
    if (currentChannel.isEmpty) throw new IllegalStateException("first set the channel for this input")
    val until = OffsetDateTime.now.minus(lastMilliseconds, ChronoUnit.MILLIS)

    messages.filter(_.getTime.isAfter(until)).toList.asJava
  }


  override def getLastPrivateMessages(lastMilliseconds: Long): java.util.List[TwitchChatMessage] = {
    val until = OffsetDateTime.now.minus(lastMilliseconds, ChronoUnit.MILLIS)

    privateMessages.filter(_.getTime.isAfter(until)).toList.asJava
  }

  override def setChannel(channel: String): Unit = {
    currentChannel = Some(TwitchChatConnector.formatChannel(channel.trim))
    if (!sourceConnector.get.isJoined(currentChannel.get)) sourceConnector.get.joinChannel(currentChannel.get)
  }

  override def getUsername: String = sourceConnector.get.getUsername

  override def ignoreOwnMessages(ignore: Boolean): Unit = this.ignoreOwnMessages = ignore

  /**
    * Stops the input, called before source connector will shutdown
    *
    * @return true if stopping was successful
    */
  override def stop(): Boolean = {
    sourceConnector.get.unregisterAllEventListeners
    true
  }
}