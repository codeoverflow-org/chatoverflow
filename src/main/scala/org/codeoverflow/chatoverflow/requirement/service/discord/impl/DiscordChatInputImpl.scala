package org.codeoverflow.chatoverflow.requirement.service.discord.impl

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util
import java.util.Optional
import java.util.concurrent.{CompletableFuture, Future, TimeUnit}
import java.util.function.Consumer

import net.dv8tion.jda.core.entities._
import net.dv8tion.jda.core.events.message.react.{GenericMessageReactionEvent, MessageReactionAddEvent, MessageReactionRemoveEvent}
import net.dv8tion.jda.core.events.message.{MessageDeleteEvent, MessageReceivedEvent, MessageUpdateEvent}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.dto.chat.ChatMessageAuthor
import org.codeoverflow.chatoverflow.api.io.dto.chat.discord._
import org.codeoverflow.chatoverflow.api.io.event.chat.discord._
import org.codeoverflow.chatoverflow.api.io.input.chat.DiscordChatInput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.impl.EventInputImpl
import org.codeoverflow.chatoverflow.requirement.service.discord.DiscordChatConnector

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.compat.java8.OptionConverters._
import scala.reflect.ClassTag

/**
  * This is the implementation of the discord chat input, using the discord connector.
  */
@Impl(impl = classOf[DiscordChatInput], connector = classOf[DiscordChatConnector])
class DiscordChatInputImpl extends EventInputImpl[DiscordEvent, DiscordChatConnector] with DiscordChatInput with WithLogger {

  private val messages = ListBuffer[DiscordChatMessage]()
  private val privateMessages = ListBuffer[DiscordChatMessage]()
  private var channelId: Option[String] = None

  override def start(): Boolean = {
    sourceConnector.get.addMessageReceivedListener(onMessage)
    sourceConnector.get.addMessageUpdateListener(onMessageUpdate)
    sourceConnector.get.addMessageDeleteListener(onMessageDelete)
    sourceConnector.get.addReactionAddEventListener(onReactionAdded)
    sourceConnector.get.addReactionDelEventListener(onReactionRemoved)
    true
  }

  override def retrieveMessage(messageId: String): Future[Optional[DiscordChatMessage]] = {
    val future = new CompletableFuture[Optional[DiscordChatMessage]]()
    val cM: Consumer[Message] = message => future.complete(Option(message).map(DiscordChatInputImpl.parse).asJava)
    val cT: Consumer[Throwable] = exception => future.completeExceptionally(exception)
    sourceConnector.get.retrieveMessage(channelId.get, messageId).queue(cM, cT)
    future
  }

  override def getLastMessages(lastMilliseconds: Long): java.util.List[DiscordChatMessage] = {
    val until = OffsetDateTime.now.minus(lastMilliseconds, ChronoUnit.MILLIS)

    messages.filter(_.getTime.isAfter(until)).toList.asJava
  }

  override def getLastPrivateMessages(lastMilliseconds: Long): util.List[DiscordChatMessage] = {
    val until = OffsetDateTime.now.minus(lastMilliseconds, ChronoUnit.MILLIS)

    privateMessages.filter(_.getTime.isAfter(until)).toList.asJava
  }

  override def getChannelId: String =
    channelId.getOrElse(throw new IllegalStateException("first set the channel for this input"))

  override def setChannel(channelId: String): Unit = {
    sourceConnector.get.getTextChannel(channelId) match {
      case Some(_) => this.channelId = Some(channelId.trim)
      case None => throw new IllegalArgumentException("TextChannel with that id doesn't exist")
    }
  }

  override def getMessage(messageId: String): Optional[DiscordChatMessage] =
    messages.find(_.getId == messageId).orElse(privateMessages.find(_.getId == messageId)).asJava

  /**
    * Stops the input, called before source connector will shutdown
    *
    * @return true if stopping was successful
    */
  override def stop(): Boolean = true

  /**
    * Listens for received messages, parses the data, adds them to the buffer and handles them over to the correct handler
    *
    * @param event a event with an new message
    */
  private def onMessage(event: MessageReceivedEvent): Unit = {
    if (channelId.isDefined) {
      if (event.getMessage.getType == MessageType.DEFAULT) {
        event.getChannelType match {
          case ChannelType.TEXT if event.getTextChannel.getId == channelId.get =>
            val message = DiscordChatInputImpl.parse(event.getMessage)
            messages += message
            call(new DiscordChatMessageSendEvent(message))
          case ChannelType.PRIVATE =>
            val message = DiscordChatInputImpl.parse(event.getMessage)
            privateMessages += message
            call(new DiscordPrivateChatMessageSendEvent(message))
          case _ => //Unknown channel, do nothing
        }
      }
    }
  }

  /**
    * Listens for edited messages, parses the data, edits the buffer and handles them over to the correct handler
    *
    * @param event a event with an edited message
    */
  private def onMessageUpdate(event: MessageUpdateEvent): Unit = {
    if (channelId.isDefined) {
      if (event.getMessage.getType == MessageType.DEFAULT) {
        event.getChannelType match {
          case ChannelType.TEXT if event.getTextChannel.getId == channelId.get =>
            val newMessage = DiscordChatInputImpl.parse(event.getMessage)
            val i = messages.indexWhere(_.getId == newMessage.getId)
            if (i != -1) {
              val oldMessage = messages(i)
              messages.update(i, newMessage)
              call(new DiscordChatMessageEditEvent(newMessage, oldMessage))
            }
          case ChannelType.PRIVATE =>
            val newMessage = DiscordChatInputImpl.parse(event.getMessage)
            val i = privateMessages.indexWhere(_.getId == newMessage.getId)
            if (i != -1) {
              val oldMessage = privateMessages(i)
              privateMessages.update(i, newMessage)
              call(new DiscordPrivateChatMessageEditEvent(newMessage, oldMessage))
            }
          case _ => //Unknown channel, do nothing
        }
      }
    }
  }

  /**
    * Listens for deleted messages, removes them from the buffer and handles them over to the correct handler
    *
    * @param event a event with an deleted message
    */
  private def onMessageDelete(event: MessageDeleteEvent): Unit = {
    if (channelId.isDefined) {
      event.getChannelType match {
        case ChannelType.TEXT if event.getTextChannel.getId == channelId.get =>
          val i = messages.indexWhere(_.getId == event.getMessageId)
          if (i != -1) {
            val oldMessage = messages.remove(i)
            call(new DiscordChatMessageDeleteEvent(oldMessage))
          }
        case ChannelType.PRIVATE =>
          val i = privateMessages.indexWhere(_.getId == event.getMessageId)
          if (i != -1) {
            val oldMessage = privateMessages.remove(i)
            call(new DiscordPrivateChatMessageDeleteEvent(oldMessage))
          }
        case _ => //Unknown channel, do nothing
      }
    }
  }

  /**
    * Listens for reactions that are added to messages. Updates the buffer and hands them over to the correct handler
    *
    * @param event a event with the added reaction and the message
    */
  private def onReactionAdded(event: MessageReactionAddEvent): Unit = {
    if (channelId.isDefined) {
      event.getChannelType match {
        case ChannelType.TEXT if event.getTextChannel.getId == channelId.get =>
          onReaction(event)(messages)((m, r) => new DiscordReactionAddEvent(m, r))
        case ChannelType.PRIVATE =>
          onReaction(event)(privateMessages)((m, r) => new DiscordReactionAddEvent(m, r))
        case _ => //Unknown channel, do nothing
      }
    }
  }

  /**
    * Listens for reactions that are removed from messages. Updates the buffer and hands them over to the correct handler
    *
    * @param event a event with the removed reaction and the message
    */
  private def onReactionRemoved(event: MessageReactionRemoveEvent): Unit = {
    if (channelId.isDefined) {
      event.getChannelType match {
        case ChannelType.TEXT if event.getTextChannel.getId == channelId.get =>
          onReaction(event)(messages)((m, r) => new DiscordReactionRemoveEvent(m, r))
        case ChannelType.PRIVATE =>
          onReaction(event)(privateMessages)((m, r) => new DiscordReactionRemoveEvent(m, r))
        case _ => //Unknown channel, do nothing
      }
    }
  }

  /**
    * Helper function for handling reaction events. Helps to reduce the amount of duplicated code
    *
    * @param event        a event with the reaction and the message
    * @param buffer       which buffer should be used for handling this event
    * @param eventCreator function that returns the reaction event that should be called
    */
  private def onReaction[T <: DiscordReactionEvent](event: GenericMessageReactionEvent)
                                                   (buffer: ListBuffer[DiscordChatMessage])
                                                   (eventCreator: (DiscordChatMessage, DiscordReaction) => T)
                                                   (implicit ct: ClassTag[T]): Unit = {
    val i = buffer.indexWhere(_.getId == event.getMessageId)
    val message =
      if (i != -1) {
        Some(buffer(i))
      } else {
        retrieveMessage(event.getMessageId).get(3, TimeUnit.SECONDS).asScala
      }
    if (message.isDefined) {
      if (i == -1) buffer += message.get else buffer.update(i, message.get)
      val reaction = DiscordChatInputImpl.parseReaction(event.getReaction)
      call(eventCreator(message.get, reaction))
    }
  }
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
            new ChatMessageAuthor(member.getEffectiveName, member.getUser.getId, "#%02X%02X%02X".format(c.getRed, c.getBlue, c.getGreen))
          case None =>
            new ChatMessageAuthor(member.getEffectiveName, member.getUser.getId)
        }
      case None =>
        new ChatMessageAuthor(message.getAuthor.getName, message.getAuthor.getId)
    }
    val channel = message.getChannel match {
      case c: TextChannel => new DiscordTextChannel(c.getName, c.getId, Option(c.getTopic).getOrElse(""))
      case c: PrivateChannel => new DiscordTextChannel(c.getName, c.getId)
    }
    val embed = parseEmbed(message.getEmbeds)
    val attachments = message.getAttachments.asScala.map(_.getUrl).asJava
    val timestamp = message.getCreationTime
    val emotes = DiscordChatInputImpl.listEmotes(message).asJava
    val reactions = message.getReactions.asScala.map(parseReaction).asJava
    new DiscordChatMessage(author, msg, timestamp, channel, emotes, embed, attachments, reactions, id)
  }

  /**
    * Creates a DiscordEmbed from the first element of the list
    *
    * @param embeds the list of message embeds from jda (may likely be empty, only first one will be parsed, the others are ignored)
    * @return the DiscordEmbed for the api
    */
  private def parseEmbed(embeds: util.List[MessageEmbed]): DiscordEmbed = {
    if (embeds.isEmpty) {
      null
    } else {
      val e = embeds.get(0)
      val title = e.getTitle
      val description = e.getDescription
      val url = e.getUrl
      val color = Option(e.getColor).map(c => "#%02X%02X%02X".format(c.getRed, c.getBlue, c.getGreen)).orNull
      val timestamp = Option(e.getTimestamp).orNull
      val (footerText, footerIconUrl) = Option(e.getFooter).map(f => (f.getText, f.getIconUrl)).getOrElse((null, null))
      val thumbnailUrl = Option(e.getThumbnail).map(_.getUrl).orNull
      val imageUrl = Option(e.getImage).map(_.getUrl).orNull
      val (auhthorName, auhthorUrl, auhthorIconUrl) = Option(e.getAuthor)
        .map(a => (a.getName, a.getUrl, a.getIconUrl)).getOrElse((null, null, null))
      val fields = e.getFields.asScala.map(f => new DiscordEmbed.Field(f.getName, f.getValue, f.isInline)).asJava
      new DiscordEmbed(title, description, url, color, timestamp, footerIconUrl,
        footerText, thumbnailUrl, imageUrl, auhthorName, auhthorUrl, auhthorIconUrl, fields)
    }
  }

  /**
    * Creates a DiscordReaction from the data provided by this MessageReaction
    *
    * @param messageReaction the MesssageReaction object returned by jda
    * @return the DiscordReaction for the chatoverflow API
    */
  private def parseReaction(messageReaction: MessageReaction): DiscordReaction = {
    val reactionEmote = messageReaction.getReactionEmote
    val count = try messageReaction.getCount catch {
      case _: IllegalStateException => 1
    }
    if (reactionEmote.isEmote) {
      val asString = s":${reactionEmote.getName}:"
      new DiscordReaction(reactionEmote.getId, asString, count, reactionEmote.getEmote.isAnimated)
    } else {
      new DiscordReaction(reactionEmote.getName, count)
    }
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
      val content = message.getContentRaw
      var index = content.indexOf(emote.getAsMention)
      while (index != -1) {
        index = content.indexOf(emote.getAsMention)
        emotes += new DiscordChatCustomEmoticon(emote.getName, index, emote.isAnimated, emote.getId)
      }
    }
    emotes.toList
  }
}

