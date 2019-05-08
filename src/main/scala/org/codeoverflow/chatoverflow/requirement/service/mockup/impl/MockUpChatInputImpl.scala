package org.codeoverflow.chatoverflow.requirement.service.mockup.impl

import java.util.Calendar
import java.util.function.Consumer

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.input.chat.{ChatMessage, MockUpChatInput}
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.Connection
import org.codeoverflow.chatoverflow.requirement.service.mockup.MockUpChatConnector

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

@Impl(impl = classOf[MockUpChatInput], connector = classOf[MockUpChatConnector])
class MockUpChatInputImpl extends Connection[MockUpChatConnector] with MockUpChatInput with WithLogger {

  // TODO: Rewrite code to fit to the new framework style using actors, a new parser, etc.

  private val messages: ListBuffer[ChatMessage] = ListBuffer[ChatMessage]()
  private val privateMessages: ListBuffer[ChatMessage] = ListBuffer[ChatMessage]()

  private val messageHandler = ListBuffer[Consumer[ChatMessage]]()
  private val privateMessageHandler = ListBuffer[Consumer[ChatMessage]]()

  override def getLastMessages(lastMilliseconds: Long): java.util.List[ChatMessage] = {
    val currentTime = Calendar.getInstance.getTimeInMillis

    messages.filter(_.getTimestamp > currentTime - lastMilliseconds).toList.asJava
  }


  override def getLastPrivateMessages(lastMilliseconds: Long): java.util.List[ChatMessage] = {
    val currentTime = Calendar.getInstance.getTimeInMillis

    privateMessages.filter(_.getTimestamp > currentTime - lastMilliseconds).toList.asJava
  }

  override def registerMessageHandler(handler: Consumer[ChatMessage]): Unit = messageHandler += handler

  override def registerPrivateMessageHandler(handler: Consumer[ChatMessage]): Unit = privateMessageHandler += handler

  override def init(): Unit = {
    if (sourceConnector.isDefined) {
      sourceConnector.get.addMessageEventListener(onMessage)
      // FIXME: Work with private messages
      sourceConnector.get.init()
    } else {
      logger warn "Source connector not set."
    }
  }

  private def onMessage(msg: ChatMessage): Unit = {
    messageHandler.foreach(consumer => consumer.accept(msg))
    messages += msg
  }

  override def serialize(): String = getSourceIdentifier

  override def deserialize(value: String): Unit = setSourceConnector(value)
}
