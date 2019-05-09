package org.codeoverflow.chatoverflow.requirement.service.saves.chat.impl

import java.util
import java.util.Calendar
import java.util.function.Consumer

import org.codeoverflow.chatoverflow.api.io.input.chat.{ChatInput, ChatMessage}
import org.codeoverflow.chatoverflow.requirement.Connection
import org.codeoverflow.chatoverflow.requirement.service.saves.chat.SavedChatConnector

import org.codeoverflow.chatoverflow.registry.Impl
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._

// TODO does this impl work ?
//@Impl(impl = classOf[ChatInput[T]], connector = classOf[SavedChatConnector[T]])
class SavedChatInputImpl[T <: ChatMessage] extends Connection[SavedChatConnector[T]] with ChatInput[T] {

  private val messages: ListBuffer[T] = ListBuffer[T]()
  private val privateMessages: ListBuffer[T] = ListBuffer[T]()
  private val messageHandler = ListBuffer[Consumer[T]]()
  private val privateMessageHandler = ListBuffer[Consumer[T]]()

  override def getLastMessages(lastMilliseconds: Long): util.List[T] = {
    val currentTime = Calendar.getInstance.getTimeInMillis

    messages.filter(_.getTimestamp > currentTime - lastMilliseconds).toList.asJava
  }

  override def getLastPrivateMessages(lastMilliseconds: Long): util.List[T] = {
    val currentTime = Calendar.getInstance.getTimeInMillis

    privateMessages.filter(_.getTimestamp > currentTime - lastMilliseconds).toList.asJava

  }

  override def registerMessageHandler(handler: Consumer[T]): Unit = messageHandler += handler

  override def registerPrivateMessageHandler(handler: Consumer[T]): Unit = privateMessageHandler += handler

  override def init(): Unit = {
    if (sourceConnector.isDefined) {
      sourceConnector.get.addMessageEventListener(onMessage)
      // FIXME: Work with private messages
      sourceConnector.get.init()
    } else {
      logger warn "Source connector not set."
    }
  }

  private def onMessage(msg: T): Unit = {
    messageHandler.foreach(consumer => consumer.accept(msg))
    messages += msg
  }

  override def serialize(): String = getSourceIdentifier

  override def deserialize(value: String): Unit = setSourceConnector(value)
}
