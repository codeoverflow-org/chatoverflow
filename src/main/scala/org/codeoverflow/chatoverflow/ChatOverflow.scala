package org.codeoverflow.chatoverflow

import java.security.Policy

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.framework.{PluginFramework, PluginManagerImpl, SandboxSecurityPolicy}
import org.codeoverflow.chatoverflow.registry.PluginRegistry

object ChatOverflow {

  val pluginFolder = "plugins/"
  private val logger = Logger.getLogger(this.getClass)

  /**
    * Returns all currently loaded plugins
    *
    * @return a list of plugin declarations (pluginAuthor,pluginName)
    */
  def getPlugins: List[(String, String)] = null

  def main(args: Array[String]): Unit = {
    println("Minzig!")

    logger info "Started Chat Overflow Framework. Hello everybody!"

    // TODO: Beginning here, a lot has to be made. More input (e.g. arguments), more output (e.g. web interface), ...

    // Create plugin framework and manager instance
    val pluginFramework = PluginFramework(pluginFolder)
    val pluginManager = new PluginManagerImpl
    val pluginRegistry = new PluginRegistry(pluginManager)

    // Create sandbox environment for plugins
    Policy.setPolicy(new SandboxSecurityPolicy)
    System.setSecurityManager(new SecurityManager)

    // Initialize plugin framework
    pluginFramework.init(pluginManager)
    logger info s"Loaded plugins list:\n ${pluginFramework.getLoadedPlugins.mkString("\n")}"

    if (pluginFramework.getNotLoadedPlugins.nonEmpty) {
      logger info s"Unable to load:\n ${pluginFramework.getNotLoadedPlugins.mkString("\n")}"
    }

    // Add a new test plugin
    val testPlug = pluginFramework.getPluggable(pluginFramework.getLoadedPlugins.filter(info => info.name == "simpletest").head)
    println(testPlug)

    // Work with plugin registry
    pluginRegistry.addPlugin("supercoolinstance1", testPlug)
    pluginRegistry.addPlugin("supercoolinstance2", testPlug)
    println(pluginRegistry.getPlugins.mkString(", "))

    val config = pluginRegistry.getConfiguration("supercoolinstance1")

    // Put shit together


  }
}
