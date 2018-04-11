package org.codeoverflow.chatoverflow.framework

import java.io.File

import org.codeoverflow.chatoverflow.api.plugin.{Pluggable, Plugin}

class PluginFramework(val pluginDirectoryPath: String) {
  val pluginDirectory = new File(pluginDirectoryPath)

  def init(): Unit = ???

  def getLoadedPlugins: Seq[Plugin] = ???

  private def loadPluggables(): Seq[Pluggable] = ???

  private def checkAndLoadPlugin(pluggable: Pluggable): Plugin = ???

  private def isMajorAPIVersionValid(pluggable: Pluggable): Boolean = ???

  private def isMinorAPIVersionValid(pluggable: Pluggable): Boolean = ???

  private def prittyPrintPluggable(pluggable: Pluggable): String = ???

  private def loadPlugin(pluggable: Pluggable): Plugin = ???

  private def asyncStartPlugin(plugin: Plugin): Boolean = ???

  // TODO: Implement a cool way to store pluggable infos + (loaded) plugin reference without the getPlugin() part
  // TODO: Alternative: Cut the plugin part from pluggable, provide a method to instanciate the plugin class later
  // TODO: Unfortunately, this choice needs a fully implemented plugin framework to test things out :)

}

object PluginFramework {

  def apply(pluginDirectoryPath: String): PluginFramework = new PluginFramework(pluginDirectoryPath)

}