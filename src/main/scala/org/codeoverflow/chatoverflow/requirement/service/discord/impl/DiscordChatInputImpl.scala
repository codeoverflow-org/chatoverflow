package org.codeoverflow.chatoverflow.requirement.service.discord.impl

import java.awt.Color
import java.util
import java.util.Calendar
import java.util.function.{BiConsumer, Consumer}

import net.dv8tion.jda.api.entities.{ChannelType, Message, MessageType, PrivateChannel, TextChannel}
import net.dv8tion.jda.api.events.message.{MessageDeleteEvent, MessageReceivedEvent, MessageUpdateEvent}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.dto.chat.discord.{DiscordChannel, DiscordChatCustomEmoticon, DiscordChatMessage, DiscordChatMessageAuthor}
import org.codeoverflow.chatoverflow.api.io.input.chat.DiscordChatInput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.Connection
import org.codeoverflow.chatoverflow.requirement.service.discord.DiscordChatConnector

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/**
  * This is the implementation of the discord chat input, using the discord connector.
  */
@Impl(impl = classOf[DiscordChatInput], connector = classOf[DiscordChatConnector])
class DiscordChatInputImpl extends Connection[DiscordChatConnector] with DiscordChatInput with WithLogger {

  private var channelId = getSourceIdentifier
  private val messages: ListBuffer[DiscordChatMessage] = ListBuffer[DiscordChatMessage]()
  private val privateMessages: ListBuffer[DiscordChatMessage] = ListBuffer[DiscordChatMessage]()
  private val messageHandler = ListBuffer[Consumer[DiscordChatMessage]]()
  private val privateMessageHandler = ListBuffer[Consumer[DiscordChatMessage]]()
  private val messageEditHandler = ListBuffer[BiConsumer[DiscordChatMessage, DiscordChatMessage]]()
  private val messageDeleteHandler = ListBuffer[Consumer[DiscordChatMessage]]()
  private val privateMessageEditHandler = ListBuffer[BiConsumer[DiscordChatMessage, DiscordChatMessage]]()
  private val privateMessageDeleteHandler = ListBuffer[Consumer[DiscordChatMessage]]()

  override def init(): Boolean = {
    if (sourceConnector.isDefined) {
      if (sourceConnector.get.isRunning || sourceConnector.get.init()) {
        setChannel(getSourceIdentifier)
        sourceConnector.get.addMessageReceivedListener(onMessage)
        sourceConnector.get.addMessageUpdateListener(onMessageUpdate)
        sourceConnector.get.addMessageDeleteListener(onMessageDelete)
        true
      } else false
    } else {
      logger warn "Source connector not set."
      false
    }
  }

  /**
    * Listens for received messages, parses the data, adds them to the buffer and handles them over to the correct handler
    *
    * @param event a event with an new message
    */
  private def onMessage(event: MessageReceivedEvent): Unit = {
    if (event.getMessage.getType == MessageType.DEFAULT) {
      val message = DiscordChatInputImpl.parse(event.getMessage)
      event.getChannelType match {
        case ChannelType.TEXT if event.getTextChannel.getId == channelId =>
          messageHandler.foreach(_.accept(message))
          messages += message
        case ChannelType.PRIVATE =>
          privateMessageHandler.foreach(_.accept(message))
          privateMessages += message
        case _ => //Unknown channel, do nothing
      }
    }
  }

  /**
    * Listens for edited messages, parses the data, edits the buffer and handles them over to the correct handler
    *
    * @param event a event with an edited message
    */
  private def onMessageUpdate(event: MessageUpdateEvent): Unit = {
    if (event.getMessage.getType == MessageType.DEFAULT) {
      val newMessage = DiscordChatInputImpl.parse(event.getMessage)
      event.getChannelType match {
        case ChannelType.TEXT =>
          val i = messages.indexWhere(_.getId == newMessage.getId)
          if (i != -1) {
            val oldMessage = messages(i)
            messages.update(i, newMessage)
            messageEditHandler.foreach(_.accept(oldMessage, newMessage))
          }
        case ChannelType.PRIVATE =>
          val i = privateMessages.indexWhere(_.getId == newMessage.getId)
          if (i != -1) {
            val oldMessage = messages(i)
            privateMessages.update(i, newMessage)
            privateMessageEditHandler.foreach(_.accept(oldMessage, newMessage))
          }
        case _ => //Unknown channel, do nothing
      }
    }
  }

  /**
    * Listens for deleted messages, removes them from the buffer and handles them over to the correct handler
    *
    * @param event a event with an deleted message
    */
  private def onMessageDelete(event: MessageDeleteEvent): Unit = {
    val id = event.getMessageId
    event.getChannelType match {
      case ChannelType.TEXT if event.getTextChannel.getId == channelId =>
        val i = messages.indexWhere(_.getId == id)
        if (i != -1) {
          val oldMessage = messages.remove(i)
          messageDeleteHandler.foreach(_.accept(oldMessage))
        }
      case ChannelType.PRIVATE =>
        val i = privateMessages.indexWhere(_.getId == id)
        if (i != -1) {
          val oldMessage = privateMessages.remove(i)
          privateMessageDeleteHandler.foreach(_.accept(oldMessage))
        }
    }
  }

  override def getLastMessages(lastMilliseconds: Long): java.util.List[DiscordChatMessage] = {
    val currentTime = Calendar.getInstance.getTimeInMillis

    messages.filter(_.getTimestamp > currentTime - lastMilliseconds).toList.asJava
  }

  override def getLastPrivateMessages(lastMilliseconds: Long): util.List[DiscordChatMessage] = {
    val currentTime = Calendar.getInstance.getTimeInMillis

    privateMessages.filter(_.getTimestamp > currentTime - lastMilliseconds).toList.asJava
  }
  override def registerMessageHandler(handler: Consumer[DiscordChatMessage]): Unit = messageHandler += handler

  override def registerPrivateMessageHandler(handler : Consumer[DiscordChatMessage]): Unit = privateMessageHandler += handler

  override def registerMessageEditHandler(handler: BiConsumer[DiscordChatMessage, DiscordChatMessage]): Unit = messageEditHandler += handler

  override def registerPrivateMessageEditHandler(handler: BiConsumer[DiscordChatMessage, DiscordChatMessage]): Unit = privateMessageEditHandler += handler

  override def registerMessageDeleteHandler(handler: Consumer[DiscordChatMessage]): Unit = messageDeleteHandler += handler

  override def registerPrivateMessageDeleteHandler(handler: Consumer[DiscordChatMessage]): Unit = privateMessageDeleteHandler += handler

  override def setChannel(channelId: String): Unit = {
    sourceConnector.get.getTextChannel(channelId) match {
      case Some(_) => this.channelId = channelId
      case None => throw new IllegalArgumentException("Channel with that id doesn't exist")
    }
  }

  override def getChannelId: String = channelId

  override def serialize(): String = getSourceIdentifier

  override def deserialize(value: String): Unit = {
    setSourceConnector(value)
  }

  override def getMessage(messageId: String): DiscordChatMessage =
    messages.find(_.getId == messageId).getOrElse(privateMessages.find(_.getId == messageId).orNull)
}

object DiscordChatInputImpl {

  /**
    * Creates a DiscordChatMessage from the data provided by this message
    *
    * @param message the discord message object returned by jda
    * @return the DiscordChatMessage of the message for work with the api
    */
  private def parse(message: Message): DiscordChatMessage = {
    val msg = message.getContentRaw
    val id = message.getId
    val author = Option(message.getMember) match {
      case Some(member) =>
        Option(message.getMember.getColor) match {
          case Some(c) =>
            new DiscordChatMessageAuthor(member.getEffectiveName, member.getId, "#%02X%02X%02X".format(c.getRed, c.getBlue, c.getGreen))
          case None =>
            new DiscordChatMessageAuthor(member.getEffectiveName, member.getId)
        }
      case None =>
        new DiscordChatMessageAuthor(message.getAuthor.getName, message.getAuthor.getId)
    }
    val channel = message.getChannel match {
      case c: TextChannel => new DiscordChannel(c.getName, c.getId, Option(c.getTopic).getOrElse(""))
      case c: PrivateChannel => new DiscordChannel(c.getName, c.getId)
    }
    val timestamp = message.getTimeCreated.toInstant.toEpochMilli
    val emotes = DiscordChatInputImpl.listEmotes(message).asJava
    new DiscordChatMessage(author, msg, timestamp, channel, emotes, id)
  }

  /**
    * Parses the emotes of a discord message into a list
    *
    * @param message the discord message object returned by jda
    * @return the DiscordChatCustomEmoticon of the message for work with the api
    */
  private def listEmotes(message: Message): List[DiscordChatCustomEmoticon] = {
    val emotes = ListBuffer[DiscordChatCustomEmoticon]()
    for (emote <- message.getEmotes.asScala if !emote.isFake) {
      val content =  message.getContentRaw
      var index = content.indexOf(emote.getAsMention)
      while (index != -1) {
        index = content.indexOf(emote.getAsMention)
        emotes += new DiscordChatCustomEmoticon(emote.getName, index, emote.isAnimated, emote.getId)
      }
    }
    emotes.toList
  }
}

