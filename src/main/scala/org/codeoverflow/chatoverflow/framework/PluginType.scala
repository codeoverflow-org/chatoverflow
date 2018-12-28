package org.codeoverflow.chatoverflow.framework

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.APIVersion
import org.codeoverflow.chatoverflow.api.plugin.{Pluggable, Plugin, PluginManager}
import org.codeoverflow.chatoverflow.framework.PluginCompatibilityState.PluginCompatibilityState

/**
  * A plugin type is a container for a pluggable definition found in a plugin jar file.
  * The plugins functionality and meta information can be accessed trough this interface.
  *
  * @param pluggable the original pluggable element, instantiated with the custom class loader
  */
class PluginType(pluggable: Pluggable) extends WithLogger {
  private var pluginVersionState = PluginCompatibilityState.Untested

  /**
    * Returns the author name of the plugin.
    *
    * @return the real name or a alias of the author
    */
  def getAuthor: String = pluggable.getAuthor

  /**
    * Returns a description of the plugin.
    *
    * @return a simple description of the service
    */
  def getDescription: String = pluggable.getDescription

  /**
    * Creates a instance of the plugins functionality only if it might be compatible.
    * For this, the plugin state is checked or tested.
    *
    * @param manager The plugin manager for framework-plugin communication
    * @return the instantiated plugin or none if the state is not compatible or the init process crashes
    */
  def createPluginInstance(manager: PluginManager): Option[Plugin] = {
    if (getState == PluginCompatibilityState.Untested) {
      testState
    }

    if (getState == PluginCompatibilityState.MajorCompatible || getState == PluginCompatibilityState.FullyCompatible) {
      try {
        val plugin = pluggable.createNewPluginInstance(manager)
        logger info s"Successful created a instance of plugin $getName ($getAuthor)"
        Some(plugin)
      } catch {
        case _: Exception =>
          logger error s"Exception thrown while creating instance of plugin $getName ($getAuthor)"
          None
      }
    } else {
      logger debug s"Unable to create instance of plugin type $getName ($getAuthor) due to different API Versions."
      None
    }
  }

  /**
    * Returns the state of of the plugin (if its API Version is compatible)
    *
    * @return a plugin version state object filled with the current status information
    */
  def getState: PluginCompatibilityState = pluginVersionState

  /**
    * Tests the plugins API Version against the framework API version and returns the compatibility state.
    *
    * @return the computed compatibility state
    */
  def testState: PluginCompatibilityState = {
    if (getMajorAPIVersion != APIVersion.MAJOR_VERSION) {
      logger info s"PluginType '$getName' has different major API version: $getMajorAPIVersion."
      pluginVersionState = PluginCompatibilityState.NotCompatible

    } else if (getMinorAPIVersion != APIVersion.MINOR_VERSION) {
      logger info s"PluginType '$getName' has different minor API version: $getMinorAPIVersion."
      pluginVersionState = PluginCompatibilityState.MajorCompatible

    } else {
      logger info s"PluginType '$getName' has no difference in API version numbers. That's good!"
      pluginVersionState = PluginCompatibilityState.FullyCompatible
    }

    pluginVersionState
  }

  /**
    * Returns the name of the plugin.
    *
    * @return the display name of the plugin
    */
  def getName: String = pluggable.getName

  /**
    * Returns the newest major version of the api, where the plugin was successfully tested!
    *
    * @return a version number
    */
  def getMajorAPIVersion: Int = pluggable.getMajorAPIVersion

  /**
    * Returns the newest minor version of the api, where the plugin was successfully tested!
    *
    * @return a version number
    */
  def getMinorAPIVersion: Int = pluggable.getMinorAPIVersion
}
