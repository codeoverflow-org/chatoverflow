package org.codeoverflow.chatoverflow.requirement.service.saves.chat.impl

import java.util
import java.util.Calendar
import java.util.function.Consumer

import org.codeoverflow.chatoverflow.api.io.input.chat.{TwitchChatInput, TwitchChatMessage}
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.Connection
import org.codeoverflow.chatoverflow.requirement.service.saves.chat.SavedTwitchChatConnector

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._

@Impl(impl = classOf[TwitchChatInput], connector = classOf[SavedTwitchChatConnector])
class SavedTwitchChatInputImpl extends Connection[SavedTwitchChatConnector] with TwitchChatInput{
  private val messages: ListBuffer[TwitchChatMessage] = ListBuffer[TwitchChatMessage]()
  private val privateMessages: ListBuffer[TwitchChatMessage] = ListBuffer[TwitchChatMessage]()
  private val messageHandler = ListBuffer[Consumer[TwitchChatMessage]]()
  private val privateMessageHandler = ListBuffer[Consumer[TwitchChatMessage]]()


  override def init(): Unit = {
    if (sourceConnector.isDefined) {
      sourceConnector.get.addMessageEventListener(onMessage)
      // FIXME: Work with private messages
      sourceConnector.get.init()
    } else {
      logger warn "Source connector not set."
    }
  }

  override def getLastMessages(lastMilliseconds: Long): util.List[TwitchChatMessage] = {
    val currentTime = Calendar.getInstance.getTimeInMillis

    messages.filter(_.getTimestamp > currentTime - lastMilliseconds).toList.asJava
  }

  override def getLastPrivateMessages(lastMilliseconds: Long): util.List[TwitchChatMessage] = {
    val currentTime = Calendar.getInstance.getTimeInMillis

    privateMessages.filter(_.getTimestamp > currentTime - lastMilliseconds).toList.asJava

  }

  override def registerMessageHandler(handler: Consumer[TwitchChatMessage]): Unit = messageHandler += handler

  override def registerPrivateMessageHandler(handler: Consumer[TwitchChatMessage]): Unit = privateMessageHandler += handler

  private def onMessage(msg: TwitchChatMessage): Unit = {
    messageHandler.foreach(consumer => consumer.accept(msg))
    messages += msg
  }

  override def serialize(): String = getSourceIdentifier

  override def deserialize(value: String): Unit = setSourceConnector(value)

  override def setChannel(channel: String): Unit = {
    // TODO: maybe not ignore (?)
  }
}
