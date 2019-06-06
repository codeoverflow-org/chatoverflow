package org.codeoverflow.chatoverflow.requirement.service.discord.impl

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util
import java.util.concurrent.{CompletableFuture, Future, TimeUnit}
import java.util.function.{BiConsumer, Consumer}
import java.util.{Calendar, Optional}

import net.dv8tion.jda.core.entities._
import net.dv8tion.jda.core.events.message.react.{GenericMessageReactionEvent, MessageReactionAddEvent, MessageReactionRemoveEvent}
import net.dv8tion.jda.core.events.message.{GenericMessageEvent, MessageDeleteEvent, MessageReceivedEvent, MessageUpdateEvent}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.dto.chat.ChatMessageAuthor
import org.codeoverflow.chatoverflow.api.io.dto.chat.discord._
import org.codeoverflow.chatoverflow.api.io.input.chat.DiscordChatInput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.InputImpl
import org.codeoverflow.chatoverflow.requirement.service.discord.DiscordChatConnector

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.compat.java8.OptionConverters._

/**
  * This is the implementation of the discord chat input, using the discord connector.
  */
@Impl(impl = classOf[DiscordChatInput], connector = classOf[DiscordChatConnector])
class DiscordChatInputImpl extends InputImpl[DiscordChatConnector] with DiscordChatInput with WithLogger {

  private val messages = ListBuffer[DiscordChatMessage]()
  private val privateMessages = ListBuffer[DiscordChatMessage]()
  private val messageHandler = ListBuffer[Consumer[DiscordChatMessage]]()
  private val privateMessageHandler = ListBuffer[Consumer[DiscordChatMessage]]()
  private val messageEditHandler = ListBuffer[BiConsumer[DiscordChatMessage, DiscordChatMessage]]()
  private val messageDeleteHandler = ListBuffer[Consumer[DiscordChatMessage]]()
  private val privateMessageEditHandler = ListBuffer[BiConsumer[DiscordChatMessage, DiscordChatMessage]]()
  private val privateMessageDeleteHandler = ListBuffer[Consumer[DiscordChatMessage]]()
  private val reactionAddHandler = ListBuffer[BiConsumer[DiscordReaction, DiscordChatMessage]]()
  private val reactionDelHandler = ListBuffer[BiConsumer[DiscordReaction, DiscordChatMessage]]()
  private var channelId: Option[String] = None

  override def start(): Boolean = {
    sourceConnector.get.addMessageReceivedListener(onMessage)
    sourceConnector.get.addMessageUpdateListener(onMessageUpdate)
    sourceConnector.get.addMessageDeleteListener(onMessageDelete)
    sourceConnector.get.addReactionAddEventListener(onReactionAdded)
    sourceConnector.get.addReactionDelEventListener(onReactionRemoved)
    true
  }

  override def getLastMessages(lastMilliseconds: Long): java.util.List[DiscordChatMessage] = {
    val until = OffsetDateTime.now.minus(lastMilliseconds, ChronoUnit.MILLIS)

    messages.filter(_.getTime.isAfter(until)).toList.asJava
  }

  override def getLastPrivateMessages(lastMilliseconds: Long): util.List[DiscordChatMessage] = {
    val until = OffsetDateTime.now.minus(lastMilliseconds, ChronoUnit.MILLIS)

    privateMessages.filter(_.getTime.isAfter(until)).toList.asJava
  }

  override def registerMessageHandler(handler: Consumer[DiscordChatMessage]): Unit = {
    getChannelId
    messageHandler += handler
  }

  override def registerPrivateMessageHandler(handler: Consumer[DiscordChatMessage]): Unit = privateMessageHandler += handler

  override def registerMessageEditHandler(handler: BiConsumer[DiscordChatMessage, DiscordChatMessage]): Unit = {
    getChannelId
    messageEditHandler += handler
  }

  override def registerPrivateMessageEditHandler(handler: BiConsumer[DiscordChatMessage, DiscordChatMessage]): Unit = privateMessageEditHandler += handler

  override def registerMessageDeleteHandler(handler: Consumer[DiscordChatMessage]): Unit = {
    getChannelId
    messageDeleteHandler += handler
  }

  override def getChannelId: String =
    channelId.getOrElse(throw new IllegalStateException("first set the channel for this input"))

  override def registerPrivateMessageDeleteHandler(handler: Consumer[DiscordChatMessage]): Unit = {
    getChannelId
    privateMessageDeleteHandler += handler
  }

  override def registerReactionAddHandler(handler: BiConsumer[DiscordReaction, DiscordChatMessage]): Unit = {
    getChannelId
    reactionAddHandler += handler
  }

  override def registerReactionRemoveHandler(handler: BiConsumer[DiscordReaction, DiscordChatMessage]): Unit = {
    getChannelId
    reactionDelHandler += handler
  }

  override def setChannel(channelId: String): Unit = {
    sourceConnector.get.getTextChannel(channelId) match {
      case Some(_) => this.channelId = Some(channelId.trim)
      case None => throw new IllegalArgumentException("TextChannel with that id doesn't exist")
    }
  }

  override def getMessage(messageId: String): Optional[DiscordChatMessage] =
    messages.find(_.getId == messageId).orElse(privateMessages.find(_.getId == messageId)).asJava

  override def retrieveMessage(messageId: String): Future[Optional[DiscordChatMessage]] = {
    val future = new CompletableFuture[Optional[DiscordChatMessage]]()
    val cM: Consumer[Message] = message => future.complete(Option(message).map(DiscordChatInputImpl.parse).asJava)
    val cT: Consumer[Throwable] = exception => future.completeExceptionally(exception)
    sourceConnector.get.retrieveMessage(channelId.get, messageId).queue(cM, cT)
    future
  }

  /**
    * Listens for received messages, parses the data, adds them to the buffer and handles them over to the correct handler
    *
    * @param event a event with an new message
    */
  private def onMessage(event: MessageReceivedEvent): Unit = {
    if (event.getMessage.getType == MessageType.DEFAULT) {
      smartHandler(event)(messages, messageHandler)(privateMessages, privateMessageHandler)((buffer, handler) => {
        val message = DiscordChatInputImpl.parse(event.getMessage)
        handler.foreach(_.accept(message))
        buffer += message
      })
    }
  }

  /**
    * Listens for edited messages, parses the data, edits the buffer and handles them over to the correct handler
    *
    * @param event a event with an edited message
    */
  private def onMessageUpdate(event: MessageUpdateEvent): Unit = {
    if (event.getMessage.getType == MessageType.DEFAULT) {
      smartHandler(event)(messages, messageEditHandler)(privateMessages, privateMessageEditHandler)((buffer, handler) => {
        val newMessage = DiscordChatInputImpl.parse(event.getMessage)
        val i = buffer.indexWhere(_.getId == newMessage.getId)
        if (i != -1) {
          val oldMessage = buffer(i)
          buffer.update(i, newMessage)
          handler.foreach(_.accept(oldMessage, newMessage))
        }
      })
    }
  }

  /**
    * Listens for deleted messages, removes them from the buffer and handles them over to the correct handler
    *
    * @param event a event with an deleted message
    */
  private def onMessageDelete(event: MessageDeleteEvent): Unit = {
    smartHandler(event)(messages, messageDeleteHandler)(privateMessages, privateMessageDeleteHandler)((buffer, handler) => {
      val i = buffer.indexWhere(_.getId == event.getMessageId)
      if (i != -1) {
        val oldMessage = buffer.remove(i)
        handler.foreach(_.accept(oldMessage))
      }
    })
  }

  /**
    * Listens for reactions that are added to messages. Updates the buffer and hands them over to the correct handler
    *
    * @param event a event with the added reaction and the message
    */
  private def onReactionAdded(event: MessageReactionAddEvent): Unit = {
    smartHandler(event)(messages, reactionAddHandler)(privateMessages, reactionAddHandler)(onReaction(event))
  }

  /**
    * Listens for reactions that are removed from messages. Updates the buffer and hands them over to the correct handler
    *
    * @param event a event with the removed reaction and the message
    */
  private def onReactionRemoved(event: MessageReactionRemoveEvent): Unit = {
    smartHandler(event)(messages, reactionDelHandler)(privateMessages, reactionDelHandler)(onReaction(event))
  }

  /**
    * Helper function for handling reaction events. Helps to reduce the amount of duplicated code
    *
    * @param event   a event with the reaction and the message
    * @param buffer  which buffer should be used for handling this event
    * @param handler which handler should be used for handling this event
    */
  private def onReaction(event: GenericMessageReactionEvent)
                        (buffer: ListBuffer[DiscordChatMessage],
                         handler: ListBuffer[BiConsumer[DiscordReaction, DiscordChatMessage]]): Unit = {
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
      handler.foreach(_.accept(reaction, message.get))
    }
  }

  /**
    * Helper function for handling events. Helps to reduce duplicated code.<br>
    * Determines which of the provided handlers and buffers should be used and performs the action with them
    *
    * @param event          the event that provides tha data
    * @param textBuffer     the buffer that should be used for normal text messages
    * @param textHandler    the handler that should be used for normal text messages
    * @param privateBuffer  the buffer that should be used for private text messages
    * @param privateHandler the handler that should be used for private text messages
    * @param action         action that should be performed with the given buffer and handler
    * @tparam B type of the buffer, should be ListBuffer[DiscordChatMessage]
    * @tparam H type of the handler, depends on the event that is handled
    */
  private def smartHandler[B, H](event: GenericMessageEvent)
                                (textBuffer: B, textHandler: H)
                                (privateBuffer: B, privateHandler: H)
                                (action: (B, H) => Unit): Unit = {
    if (channelId.isDefined) {
      var buffer: Option[B] = None
      var handler: Option[H] = None
      event.getChannelType match {
        case ChannelType.TEXT if event.getTextChannel.getId == channelId.get =>
          buffer = Some(textBuffer)
          handler = Some(textHandler)
        case ChannelType.PRIVATE =>
          buffer = Some(privateBuffer)
          handler = Some(privateHandler)
        case _ => //Unknown channel, do nothing
      }
      if (buffer.isDefined && handler.isDefined) {
        action(buffer.get, handler.get)
      }
    }
  }

  /**
    * Stops the input, called before source connector will shutdown
    *
    * @return true if stopping was successful
    */
  override def stop(): Boolean = true
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

