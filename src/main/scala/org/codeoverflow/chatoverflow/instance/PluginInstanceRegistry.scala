package org.codeoverflow.chatoverflow.instance

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.framework.PluginType

import scala.collection.mutable

/**
  * The plugin instance registry does hold all running and not-running instances of plugins.
  */
class PluginInstanceRegistry extends WithLogger {
  private val pluginInstances = mutable.Map[String, PluginInstance]()

  /**
    * Adds a new plugin instance container to the map of plugin instances.
    * Please note: While adding a new plugin instance to the list, the loaded plugin is instantiated.
    * If this process is not possible due to version differences or does fail, the plugin is not
    * added to the list.
    *
    * @param instanceName the name of the plugin, has to be unique
    * @param pluginType   the type of the plugin. To get a full list, have a look at PluginFramework.
    * @return true if the plugin could be instantiated and added to the list
    *         false if the name does already exist or the instantiation process failed
    */
  def addPluginInstance(instanceName: String, pluginType: PluginType): Boolean = {
    if (pluginInstances.contains(instanceName)) {
      logger debug s"Unable to add plugin instance with name '$instanceName'. Name does already exist!"
      false
    } else {

      // The plugin instance is created here. Why: A plugin, which could not be created
      // e.g. due to version issues or code failures has no reason to be in the list of plugin instances
      val instance = new PluginInstance(instanceName, pluginType)
      if (instance.createPluginInstanceWithDefaultManager) {
        pluginInstances += instanceName -> instance
        logger info s"Successfully instantiated and added plugin instance '$instanceName' of type '${pluginType.getName}'."
        true
      } else {
        logger warn s"Unable to add the plugin instance '$instanceName' of type ${pluginType.getName}. Unable to instantiate."
        false
      }
    }
  }

  /**
    * Returns the plugin with the given name, if there is any
    *
    * @param instanceName the name of the plugin instance to retrieve
    * @return some plugin instance or none
    */
  def getPluginInstance(instanceName: String): Option[PluginInstance] = {
    pluginInstances.get(instanceName)
  }

  /**
    * Removes a plugin specified by its name, if possible.
    *
    * @param instanceName the name of the instance to remove
    * @return true, if the removing process was possible
    */
  def removePluginInstance(instanceName: String): Boolean = {
    if (pluginInstances.contains(instanceName) && pluginInstances(instanceName).isRunning) {
      false
    } else {
      pluginInstances -= instanceName
      true
    }
  }

  /**
    * Returns if the registry contains a specified instanceName (key).
    *
    * @param instanceName the name to search for
    * @return true, if a plugin instance with the given name exists
    */
  def pluginInstanceExists(instanceName: String): Boolean = pluginInstances.contains(instanceName)

  /**
    * Returns a list of all plugin instances. Can be used to serialize the instance content.
    *
    * @return a list of plugin instance objects of the runtime environment
    */
  def getAllPluginInstances: List[PluginInstance] = pluginInstances.values.toList

}
