package org.codeoverflow.chatoverflow.requirement.service.saves.chat

import java.util.Calendar

import com.google.gson.{Gson, JsonParser}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.input.chat.TwitchChatMessage
import org.codeoverflow.chatoverflow.connector.Connector

import collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.io.{BufferedSource, Source}

class SavedTwitchChatConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {
  private var running = false
  private val savedChatsFolder = "src/main/resources/test" // TODO:
  private val messageListener = ListBuffer[TwitchChatMessage => Unit]()
  private var messages = ListBuffer[TwitchChatMessage]()
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
  def addMessageEventListener(listener: TwitchChatMessage => Unit): Unit = {
    messageListener += listener
  }

  def addPrivateMessageEventListener(listener: TwitchChatMessage => Unit): Unit = {
    // FIXME: Support private messages
  }

  def simulateChat(): Unit = {
    // FIXME: Should be invoked every x milliseconds using actors not using sleep
    val step = 100
    while (running) {
      val currentTime = Calendar.getInstance.getTimeInMillis

      // TODO: performance
      val lastMessages = messages.filter(msg => (msg.getTimestamp > currentTime - step) && (msg.getTimestamp <= currentTime))

      lastMessages.foreach(msg => messageListener.foreach(listener => listener(msg)))

      Thread.sleep(step)
    }
  }

  def loadSavedChatFile(): Boolean = {
    // TODO: Handle exceptions
    // FIXME: WHY (don't use a static offset you dumb brick)
    val currentTime = Calendar.getInstance().getTimeInMillis + 5000L
    val filePath = s"$savedChatsFolder/$sourceIdentifier.json"
    logger warn "Loading chat from " + filePath
    val input: BufferedSource = Source.fromFile(filePath)
    if (input.isEmpty) return false
    val parser = new JsonParser()
    var jsonElement = parser.parse(input.mkString)
    var firstMessageTime: Long = -1L
    jsonElement.getAsJsonArray.forEach(string => {
      val twitchChatMessage = gson.fromJson(string, classOf[TwitchChatMessage])
      if (firstMessageTime < 0) firstMessageTime = twitchChatMessage.getTimestamp
      val timeDifference = twitchChatMessage.getTimestamp - firstMessageTime
      val time = currentTime + timeDifference
      twitchChatMessage.setTimestamp(time)
      messages.append(twitchChatMessage)
    })
    messages.sortBy(msg => msg.getTimestamp)
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
