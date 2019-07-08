package org.codeoverflow.chatoverflow.framework.manager

import java.util

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.plugin.{PluginLogMessage, PluginManager}
import org.codeoverflow.chatoverflow.ui.web.rest.events.EventsDispatcher
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/**
  * This is the default implementation of the plugin manager used in the framework.
  *
  * @param pluginInstanceName the name of the plugin instance
  */
class PluginManagerImpl(pluginInstanceName: String, logOutputOnConsole: Boolean) extends PluginManager with WithLogger {

  private val logMessages = new ListBuffer[PluginLogMessage]

  /**
    * Prints a log message on the console and saves the message for later inspection.
    *
    * @param message the message to show
    */
  override def log(message: String): Unit = {
    val logMessage = new PluginLogMessage(message)
    logMessages += logMessage

    if (logOutputOnConsole) {
      logger info s"[$pluginInstanceName] $message"
    }

    implicit val formats: DefaultFormats.type = DefaultFormats
    val data = Map(("message", message), ("timestamp", logMessage.getTimestamp.toString))
    EventsDispatcher.broadcast("instance", Serialization.write(Map(("name", pluginInstanceName), ("action", "log"), ("data", data))))
  }

  /**
    * Returns a list of already posted log messages.
    *
    * @return a list of log messages
    */
  override def getLogMessages: util.List[PluginLogMessage] = logMessages.toList.asJava
}
