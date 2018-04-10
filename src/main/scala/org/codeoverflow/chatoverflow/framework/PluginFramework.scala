package org.codeoverflow.chatoverflow.framework

import java.io.File

import org.codeoverflow.chatoverflow.api.plugin.{Pluggable, Plugin}

class PluginFramework(val pluginDirectoryPath: String) {
  val pluginDirectory = new File(pluginDirectoryPath)

  def loadPluggables(): Seq[Pluggable] = ???

  def checkAndLoadPlugin(pluggable: Pluggable): Plugin = ???

  private def isMajorAPIVersionValid(pluggable: Pluggable): Boolean = ???

  private def isMinorAPIVersionValid(pluggable: Pluggable): Boolean = ???

  private def prittyPrintPluggable(pluggable: Pluggable): String = ???

  private def loadPlugin(pluggable: Pluggable): Plugin = ???

  private def asyncStartPlugin(plugin: Plugin): Boolean = ???

}
