package org.codeoverflow.chatoverflow.requirement.service.saves.chat

import java.io.{BufferedWriter, File, FileWriter}

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector

class SavedChatOutputConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger  {
  private var running = false
  private val directoryPath = s"src/main/resources/$sourceIdentifier"
  private val directory = new File(directoryPath)
  if (!directory.exists()) directory.mkdir()

  def save(fileName: String, content: String): Unit = {
    val filePath = s"$directoryPath/$fileName.json"
    logger warn "Writing chat to file " + filePath
    val file = new File(filePath)
    file.createNewFile()
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(content)
    bw.close()
  }

  /**
    * Returns true, if the connector has been already instantiated and is running.
    */
  override def isRunning: Boolean = running

  /**
    * Initializes the connector, e.g. creates a connection with its platform.
    */
  override def init(): Boolean = {
    if (!running) {
      running = true
      logger info s"Starting connector for source '$sourceIdentifier' of type '$getUniqueTypeString'."
      running
    }
    else {
      logger warn "Connector already running."
      false
    }
  }

  /**
    * Shuts down the connector, closes its platform connection.
    */
  override def shutdown(): Unit = {
    logger info s"Shutting down SavedChatOutputConnector for $sourceIdentifier."
    running = false
  }
}
