package org.codeoverflow.chatoverflow.registry

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.api.plugin.configuration.Requirements
import org.codeoverflow.chatoverflow.api.plugin.{Pluggable, Plugin, PluginManager}

import scala.collection.mutable

class PluginInstanceRegistry(pluginManager: PluginManager) {

  private val logger = Logger.getLogger(this.getClass)
  private val pluginInstances = mutable.Map[String, Plugin]()

  def addPluginInstance(instanceName: String, pluggable: Pluggable): Boolean = {
    if (pluginInstances.contains(instanceName)) {
      false
    } else {
      pluginInstances += instanceName -> pluggable.createNewPluginInstance(pluginManager)
      true
    }
  }

  def getPluginInstances: Seq[String] = pluginInstances.keys.toSeq

  def getRequirements(instanceName: String): Requirements = {
    pluginInstances(instanceName).getRequirements
  }

  /**
    * Creates a new thread and tries to start the plugin.
    * Make sure to set the requirements (configuration) first!
    *
    * @param instanceName the pluginName with which it was added before
    */
  def asyncStartPlugin(instanceName: String): Unit = {

    // Always escape!
    if (!pluginInstances.contains(instanceName)) {
      logger warn s"Plugin '$instanceName' was not loaded. Unable to start."
    } else {
      logger info s"Starting plugin '$instanceName' in new thread!"
      val loadedPlugin = pluginInstances(instanceName)

      if (!loadedPlugin.getRequirements.allNeededRequirementsSet()) {
        logger error s"At least one requirement of '$instanceName' has not been set. Unable to start!"
      } else {

        try {
          // TODO: Manage all threads, passing arguments, maybe using actors rather than threads
          new Thread(() => {
            try {
              loadedPlugin.start()
            } catch {
              case e: AbstractMethodError => logger.error(s"Plugin '$instanceName' just crashed. Looks like a plugin version error.", e)
              case e: Exception => logger.error(s"Plugin '$instanceName' just had an exception. Might be a plugin implementation fault.", e)
              case e: Throwable => logger.error(s"Plugin '$instanceName' just crashed.", e)
            }
          }).start()
        } catch {
          case e: Throwable => logger.error(s"Plugin starting process (Plugin: '$instanceName') just crashed.", e)
        }
      }
    }
  }

}