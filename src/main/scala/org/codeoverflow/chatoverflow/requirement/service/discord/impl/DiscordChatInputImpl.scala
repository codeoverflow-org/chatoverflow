package org.codeoverflow.chatoverflow.requirement.service.discord.impl

import java.util
import java.util.Calendar
import java.util.function.Consumer

import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.message.{MessageDeleteEvent, MessageReceivedEvent, MessageUpdateEvent}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.input.chat.{DiscordChannel, DiscordChatInput, DiscordChatMessage, DiscordChatMessageAuthor}
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.Connection
import org.codeoverflow.chatoverflow.requirement.service.discord.DiscordChatConnector

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._

/**
  * This is the implementation of the discord chat input, using the discord connector.
  */
@Impl(impl = classOf[DiscordChatInput], connector = classOf[DiscordChatConnector])
class DiscordChatInputImpl extends Connection[DiscordChatConnector] with DiscordChatInput with WithLogger {

  private val messages: ListBuffer[DiscordChatMessage] = ListBuffer[DiscordChatMessage]()
  private val privateMessages: ListBuffer[DiscordChatMessage] = ListBuffer[DiscordChatMessage]()

  private val messageHandler = ListBuffer[Consumer[DiscordChatMessage]]()
  private val privateMessageHandler = ListBuffer[Consumer[DiscordChatMessage]]()

  override def init(): Unit = {
    if (sourceConnector.isDefined) {
      sourceConnector.get.addMessageEventListener(onMessage)
      sourceConnector.get.addMessageUpdateEventListener(onMessageUpdate)
      sourceConnector.get.addMessageDeleteEventListener(onMessageDelete)
      sourceConnector.get.init()
    } else {
      logger warn "Source connector not set."
    }
  }

  private def onMessage(event: MessageReceivedEvent): Unit = {
    val message = event.getMessage.getContentRaw
    val snowflakeId = event.getMessageIdLong
    val author = new DiscordChatMessageAuthor(event.getMember.getEffectiveName, event.getMember.getIdLong)
    val channel = new DiscordChannel(event.getChannel.getName, event.getChannel.getIdLong)
    val timestamp = event.getMessage.getTimeCreated.toInstant.toEpochMilli
    val msg = new DiscordChatMessage(author, message, timestamp, channel, snowflakeId)
    event.getChannelType match {
      case ChannelType.TEXT =>
        messageHandler.foreach(consumer => consumer.accept(msg))
        messages += msg
      case ChannelType.PRIVATE =>
        privateMessageHandler.foreach(consumer => consumer.accept(msg))
        privateMessages += msg
      case _ => //Unknown channel, do nothing
    }
  }

  private def onMessageUpdate(event: MessageUpdateEvent): Unit = {
    //TODO Would it be better to change the message value rather than replace the whole object?
    val message = event.getMessage.getContentRaw
    val snowflakeId = event.getMessageIdLong
    val author = new DiscordChatMessageAuthor(event.getMember.getEffectiveName, event.getMember.getIdLong)
    val channel = new DiscordChannel(event.getChannel.getName, event.getChannel.getIdLong)
    val timestamp = event.getMessage.getTimeCreated.toInstant.toEpochMilli
    val msg = new DiscordChatMessage(author, message, timestamp, channel, snowflakeId)
    event.getChannelType match {
      case ChannelType.TEXT =>
        val i = messages.indexWhere(msg => msg.getSnowflakeId == snowflakeId)
        if (i >= 0) messages.update(i, msg)
      case ChannelType.PRIVATE =>
        val i = privateMessages.indexWhere(msg => msg.getSnowflakeId == snowflakeId)
        if (i >= 0) privateMessages.update(i, msg)
      case _ => //Unknown channel, do nothing
    }
  }

  private def onMessageDelete(event: MessageDeleteEvent): Unit = {
    val snowflakeId = event.getMessageIdLong
    event.getChannelType match {
      case ChannelType.TEXT =>
        val i = messages.indexWhere(msg => msg.getSnowflakeId == snowflakeId)
        if (i >= 0) messages.remove(i)
      case ChannelType.PRIVATE =>
        val i = privateMessages.indexWhere(msg => msg.getSnowflakeId == snowflakeId)
        if (i >= 0) privateMessages.remove(i)
      case _ => //Unknown channel, do nothing
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

  override def setChannel(channelId: Long): Unit = sourceConnector.get.setChannel(channelId)

  override def serialize(): String = getSourceIdentifier

  override def deserialize(value: String): Unit = {
    setSourceConnector(value)
  }
}