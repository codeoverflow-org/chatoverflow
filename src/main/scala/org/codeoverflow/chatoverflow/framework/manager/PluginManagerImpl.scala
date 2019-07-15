package org.codeoverflow.chatoverflow.framework.manager

import java.util

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.plugin.{PluginLogMessage, PluginManager}

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
    logMessages += new PluginLogMessage(message)

    if (logOutputOnConsole) {
      logger info s"[$pluginInstanceName] $message"
    }
  }

  /**
    * Returns a list of already posted log messages.
    *
    * @return a list of log messages
    */
  override def getLogMessages: util.List[PluginLogMessage] = logMessages.toList.asJava
}
