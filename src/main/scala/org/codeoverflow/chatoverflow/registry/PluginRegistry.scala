package org.codeoverflow.chatoverflow.registry

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.api.plugin.configuration.Configuration
import org.codeoverflow.chatoverflow.api.plugin.{Pluggable, Plugin, PluginManager}

import scala.collection.mutable

object PluginRegistry {

  private val logger = Logger.getLogger(this.getClass)
  private val plugins = mutable.Map[String, Plugin]()

  def addPlugin(pluginName: String, pluggable: Pluggable, pluginManager: PluginManager): Boolean = {
    if (plugins.contains(pluginName)) {
      false
    } else {
      plugins += pluginName -> pluggable.createNewPluginInstance(pluginManager)
      true
    }
  }

  def getPlugins: Seq[String] = plugins.keys.toSeq

  def getConfigurationForPluginWithName(pluginName: String): Configuration = {
    plugins(pluginName).getRequirements
  }

  /**
    * Creates a new thread and tries to start the plugin
    *
    * @param pluginName the pluginName with which it was added before
    */
  def asyncStartPluginWithName(pluginName: String): Unit = {

    // Always escape!
    if (!plugins.contains(pluginName)) {
      logger warn s"Plugin '$pluginName' was not loaded. Unable to start."
    } else {
      logger info s"Starting plugin '$pluginName' in new thread!"
      val loadedPlugin = plugins(pluginName)

      try {
        // TODO: Manage all threads, passing arguments, maybe using actors rather than threads
        new Thread(() => {
          try {
            loadedPlugin.start()
          } catch {
            case e: AbstractMethodError => logger.error(s"Plugin '$pluginName' just crashed. Looks like a plugin version error.", e)
            case e: Exception => logger.error(s"Plugin '$pluginName' just had an exception. Might be a plugin implementation fault.", e)
            case e: Throwable => logger.error(s"Plugin '$pluginName' just crashed.", e)
          }
        }).start()
      } catch {
        case e: Throwable => logger.error(s"Plugin starting process (Plugin: '$pluginName') just crashed.", e)
      }
    }
  }

}