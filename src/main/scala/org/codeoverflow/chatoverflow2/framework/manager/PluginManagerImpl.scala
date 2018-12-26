package org.codeoverflow.chatoverflow2.framework.manager

import java.util

import org.codeoverflow.chatoverflow.api.plugin.PluginManager
import org.codeoverflow.chatoverflow2.WithLogger

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/**
  * This is the default implementation of the plugin manager used in the framework.
  *
  * @param pluginInstanceName the name of the plugin instance
  */
class PluginManagerImpl(pluginInstanceName: String) extends PluginManager with WithLogger {

  private val logMessages = new ListBuffer[String]

  /**
    * Prints a log message on the console and saves the message for later inspection.
    *
    * @param message the message to show
    */
  override def log(message: String): Unit = {
    logMessages += message
    // TODO: Add disabling plugin messages by flag
    logger info s"[$pluginInstanceName] $message"
  }

  /**
    * Returns a list of already posted log messages.
    *
    * @return a list of log messages
    */
  override def getLogMessages: util.List[String] = logMessages.toList.asJava
}
