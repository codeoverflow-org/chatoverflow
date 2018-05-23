package org.codeoverflow.chatoverflow

import java.security.Policy

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.api.io.input.chat.TwitchChatInput
import org.codeoverflow.chatoverflow.api.plugin.configuration.{ParameterRequirement, SourceRequirement}
import org.codeoverflow.chatoverflow.framework.{PluginFramework, PluginManagerImpl, SandboxSecurityPolicy}
import org.codeoverflow.chatoverflow.io.connector.{TwitchConnector, TwitchCredentials}
import org.codeoverflow.chatoverflow.io.input.chat.TwitchChatInputImpl
import org.codeoverflow.chatoverflow.registry.{ConnectorRegistry, PluginRegistry}

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

    // ---------------------------------------------------

    // This code will be executed when you create a new plugin from the list of pluggables in the GUI
    // "I want a new "simpletest" plugin named "supercoolinstance1"
    val testPlug = pluginFramework.getPluggable(pluginFramework.getLoadedPlugins.filter(info => info.name == "simpletest").head)
    pluginRegistry.addPlugin("supercoolinstance1", testPlug)

    // This code will be executed when you create a new service in the list with given credentials
    // Either standalone or when needed from a created plugin
    val sourceId = "skate702"
    val connector = new TwitchConnector(sourceId, TwitchCredentials("skate702", "oauth:xxxx"))
    ConnectorRegistry.addConnector(connector)

    // Put shit together (kinda hacky)
    // This code will be executed during plugin configuration. This connects the connector with the plugin input
    val config = pluginRegistry.getConfiguration("supercoolinstance1")
    config.getInputs.forEach((_, value) => {
      value match {
        case value: SourceRequirement[TwitchChatInput] =>
          val input = new TwitchChatInputImpl
          input.setSource(sourceId)
          input.init()
          value.setSource(input)
      }
    })
    config.getParameters.forEach((_, value) => {
      value match {
        case value: ParameterRequirement[String] =>
          value.setParameter("Hello world!")
      }
    })

    // This starts the plugin!
    pluginRegistry.asyncStartPlugin("supercoolinstance1")
  }
}
