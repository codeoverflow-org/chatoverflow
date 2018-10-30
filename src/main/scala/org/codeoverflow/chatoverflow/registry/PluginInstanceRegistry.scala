package org.codeoverflow.chatoverflow.registry

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.api.plugin.configuration.Requirements
import org.codeoverflow.chatoverflow.api.plugin.{Pluggable, Plugin, PluginManager}
import org.codeoverflow.chatoverflow.framework.PluginManagerImpl

import scala.collection.mutable

/**
  * The plugin instance registry holds all loaded plugin instances, ready to be executed (or already running).
  */
class PluginInstanceRegistry {

  private val logger = Logger.getLogger(this.getClass)
  private val pluginInstances = mutable.Map[String, Plugin]()

  /**
    * Adds a new plugin instance with a unique name and loaded pluggable from the plugin jars from the framework.
    *
    * @param instanceName the unique plugin instance name (does not need to be the plugin name to instantiate!)
    * @param pluggable    the pluggable, already loaded by the framework
    * @return true, if the plugin could be loaded correctly
    */
  def addPluginInstance(instanceName: String, pluggable: Pluggable): Boolean = {
    if (pluginInstances.contains(instanceName)) {
      false
    } else {
      pluginInstances += instanceName -> pluggable.createNewPluginInstance(new PluginManagerImpl(instanceName))
      true
    }
  }

  /**
    * Returns a seq of names of all plugin instances.
    *
    * @return a seq of names (not the plugins themselves)
    */
  def getPluginInstances: Seq[String] = pluginInstances.keys.toSeq

  /**
    * Returns the requirements object of a plugin to fill in information from the configs.
    *
    * @param instanceName the instance name of the loaded plugin instance
    * @return a requirements object, holding requirements to fill
    */
  def getRequirements(instanceName: String): Requirements = {
    pluginInstances(instanceName).getRequirements
  }

  /**
    * Returns the plugin manager created for this specific instance.
    *
    * @param instanceName the instance name of the loaded plugin instace
    * @return a plugin manager object
    */
  def getPluginManager(instanceName: String): PluginManager = {
    pluginInstances(instanceName).getManager
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
      logger warn s"Instance '$instanceName' was not loaded. Unable to start."
    } else {
      logger info s"Starting plugin '$instanceName' in new thread!"
      val loadedPlugin = pluginInstances(instanceName)

      if (!loadedPlugin.getRequirements.allNeededRequirementsSet()) {
        logger error s"At least one non-optional requirement of '$instanceName' has not been set. Unable to start!"

        logger error s"Not set: ${loadedPlugin.getRequirements.getMissingRequirementIds.toArray.mkString(", ")}."

      } else {

        try {
          // TODO: Manage all threads, passing arguments, maybe using actors rather than threads
          new Thread(() => {
            try {

              // Execute plugin setup
              loadedPlugin.setup()

              // Execute loop, if an interval is set
              if (loadedPlugin.getLoopInterval > 0) {
                while (true) {
                  loadedPlugin.loop()
                  Thread.sleep(loadedPlugin.getLoopInterval)
                }
              }

              // FIXME: shutdown method should work
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