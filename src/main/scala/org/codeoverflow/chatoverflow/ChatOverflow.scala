package org.codeoverflow.chatoverflow

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.framework.{PluginFramework, PluginManagerImpl}

object ChatOverflow {

  val pluginFolder = "plugins/"
  private val logger = Logger.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    println("Minzig!")

    logger info "Started Chat Overflow Framework!"

    // Create plugin framework and manager instance
    val pluginFramework = PluginFramework(pluginFolder)
    val pluginManager = new PluginManagerImpl

    pluginFramework.init(pluginManager)
    println(pluginFramework.getLoadedPlugins.mkString(", "))
  }

}
