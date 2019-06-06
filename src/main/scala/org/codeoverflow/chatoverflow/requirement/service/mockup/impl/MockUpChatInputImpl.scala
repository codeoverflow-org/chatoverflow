package org.codeoverflow.chatoverflow.requirement.service.mockup.impl

import java.util.Calendar
import java.util.function.Consumer

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.dto.chat.{Channel, ChatEmoticon, ChatMessage, ChatMessageAuthor, TextChannel}
import org.codeoverflow.chatoverflow.api.io.input.chat.MockUpChatInput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.InputImpl
import org.codeoverflow.chatoverflow.requirement.service.mockup.MockUpChatConnector

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

@Deprecated
@Impl(impl = classOf[MockUpChatInput], connector = classOf[MockUpChatConnector])
class MockUpChatInputImpl extends InputImpl[MockUpChatConnector] with MockUpChatInput with WithLogger {

  private val messages: ListBuffer[ChatMessage[ChatMessageAuthor, TextChannel, ChatEmoticon]] = ListBuffer[ChatMessage[ChatMessageAuthor, TextChannel, ChatEmoticon]]()
  private val privateMessages: ListBuffer[ChatMessage[ChatMessageAuthor, TextChannel, ChatEmoticon]] = ListBuffer[ChatMessage[ChatMessageAuthor, TextChannel, ChatEmoticon]]()

  private val messageHandler = ListBuffer[Consumer[ChatMessage[ChatMessageAuthor, TextChannel, ChatEmoticon]]]()
  private val privateMessageHandler = ListBuffer[Consumer[ChatMessage[ChatMessageAuthor, TextChannel, ChatEmoticon]]]()

  override def getLastMessages(lastMilliseconds: Long): java.util.List[ChatMessage[ChatMessageAuthor, TextChannel, ChatEmoticon]] = {
    val currentTime = Calendar.getInstance.getTimeInMillis

    messages.filter(_.getTime > currentTime - lastMilliseconds).toList.asJava
  }


  override def getLastPrivateMessages(lastMilliseconds: Long): java.util.List[ChatMessage[ChatMessageAuthor, TextChannel, ChatEmoticon]] = {
    val currentTime = Calendar.getInstance.getTimeInMillis

    privateMessages.filter(_.getTime > currentTime - lastMilliseconds).toList.asJava
  }

  override def registerMessageHandler(handler: Consumer[ChatMessage[ChatMessageAuthor, TextChannel, ChatEmoticon]]): Unit = messageHandler += handler

  override def registerPrivateMessageHandler(handler: Consumer[ChatMessage[ChatMessageAuthor, TextChannel, ChatEmoticon]]): Unit = privateMessageHandler += handler

  override def serialize(): String = getSourceIdentifier

  override def deserialize(value: String): Unit = setSourceConnector(value)

  /**
    * Start the input, called after source connector did init
    *
    * @return true if starting the input was successful, false if some problems occurred
    */
  override def start(): Boolean = true

  private def onMessage(msg: ChatMessage[ChatMessageAuthor, TextChannel, ChatEmoticon]): Unit = {
    messageHandler.foreach(consumer => consumer.accept(msg))
    messages += msg
  }

  /**
    * Stops the input, called before source connector will shutdown
    *
    * @return true if stopping was successful
    */
  override def stop(): Boolean = true
}
