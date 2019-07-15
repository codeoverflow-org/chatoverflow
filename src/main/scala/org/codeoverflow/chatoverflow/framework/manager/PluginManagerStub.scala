package org.codeoverflow.chatoverflow.framework.manager

import java.util

import org.codeoverflow.chatoverflow.api.plugin.{PluginLogMessage, PluginManager}

/**
  * This plugin manager stub does not provide any useful functionality and should only be used for
  * plugin instantiation testing.
  */
class PluginManagerStub extends PluginManager {
  /**
    * Prints a log message on the console and saves the message for later inspection.
    *
    * @param message the message to show
    */
  override def log(message: String): Unit = {

  }

  /**
    * Returns a list of already posted log messages.
    *
    * @return a list of log messages
    */
  override def getLogMessages: util.List[PluginLogMessage] = {
    new util.ArrayList[PluginLogMessage]()
  }
}