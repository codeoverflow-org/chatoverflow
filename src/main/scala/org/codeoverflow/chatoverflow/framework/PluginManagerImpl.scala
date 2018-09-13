package org.codeoverflow.chatoverflow.framework

import java.util

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.api.plugin.PluginManager

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer


class PluginManagerImpl(instanceName: String) extends PluginManager {

  private val logger = Logger.getLogger(this.getClass)
  private val logMessages = new ListBuffer[String]

  override def log(message: String): Unit = {
    logMessages += message
    logger info s"[$instanceName] $message"
  }

  override def getLogMessages: util.List[String] = logMessages.toList.asJava
}
