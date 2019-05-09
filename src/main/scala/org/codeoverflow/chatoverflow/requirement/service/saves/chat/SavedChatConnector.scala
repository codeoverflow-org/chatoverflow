package org.codeoverflow.chatoverflow.requirement.service.saves.chat

import java.util.Calendar

import com.google.gson.Gson
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.input.chat.ChatMessage
import org.codeoverflow.chatoverflow.connector.Connector

import scala.collection.mutable.ListBuffer
import scala.io.{BufferedSource, Source}

abstract class SavedChatConnector[T <: ChatMessage](override val sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {
  private var running = false
  private val savedChatsFolder = "src/main/resources/savedchats"
  private val messageListener = ListBuffer[T => Unit]()
  private var messages: List[T] = _
  val gson = new Gson()
  /**
    * Returns true, if the connector has been already instantiated and is running.
    */
  override def isRunning: Boolean = running

  /**
    * Initializes the connector, e.g. creates a connection with its platform.
    */
  override def init(): Boolean = {
    if (!running) {
      logger info s"Starting connector for source '$sourceIdentifier' of type '$getUniqueTypeString'."
      running = loadSavedChatFile()
      new Thread(() => simulateChat()).start()
      running
    }
    else {
      logger warn "Connector already running."
      false
    }
  }

  def addMessageEventListener(listener: T => Unit): Unit = {
    messageListener += listener
  }

  def addPrivateMessageEventListener(listener: T => Unit): Unit = {
    // FIXME: Support private messages
  }

  def simulateChat(): Unit = {
    // FIXME: Should be invoked every x milliseconds using actors not using sleep
    val step = 100
    while (running) {
      val currentTime = Calendar.getInstance.getTimeInMillis

      val lastMessages = messages.filter(msg => (msg.getTimestamp > currentTime - step) && (msg.getTimestamp <= currentTime))

      lastMessages.foreach(msg => messageListener.foreach(listener => listener(msg)))

      Thread.sleep(step)
    }
  }

  def loadSavedChatFile(): Boolean = {
    // TODO: Handle exceptions
    // FIXME: WHY (don't use a static offset you dumb brick)
    val currentTime = Calendar.getInstance().getTimeInMillis + 5000L
    val input: BufferedSource = Source.fromFile(s"$savedChatsFolder/$sourceIdentifier.chat")
    val messages: List[T] = gson.fromJson(input.toString, classOf[List[T]])
    if (messages.size < 0) return false
    val firstMessageTime = messages.head.getTimestamp

    messages.foreach(msg => {
      val timeDifference = msg.getTimestamp - firstMessageTime
      val time = currentTime + timeDifference
      msg.setTimestamp(time)
    })
    input.close()
    true
    // TODO: Do not always return true
  }

  /**
    * Shuts down the connector, closes its platform connection.
    */
  override def shutdown(): Unit = {
    logger info s"Shutting down MockUpChatConnector for $sourceIdentifier."
    running = false
  }
}
