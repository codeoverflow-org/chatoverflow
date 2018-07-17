package org.codeoverflow.chatoverflow

import java.security.Policy

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.api.io.input.chat.TwitchChatInput
import org.codeoverflow.chatoverflow.api.plugin.configuration.{ParameterRequirement, SourceRequirement}
import org.codeoverflow.chatoverflow.config.Configuration
import org.codeoverflow.chatoverflow.framework.{PluginFramework, PluginId, PluginManagerImpl, SandboxSecurityPolicy}
import org.codeoverflow.chatoverflow.io.connector.{TwitchConnector, TwitchCredentials}
import org.codeoverflow.chatoverflow.io.input.chat.TwitchChatInputImpl
import org.codeoverflow.chatoverflow.registry.{ConnectorRegistry, PluginRegistry}

object ChatOverflow {

  val pluginFolderPath = "plugins/"
  val configFolderPath = "config/"
  private val logger = Logger.getLogger(this.getClass)

  private var pluginFramework: PluginFramework = _
  private var pluginRegistry: PluginRegistry = _
  private var configuration: Configuration = _

  def main(args: Array[String]): Unit = {
    println("Minzig!")
    logger info "Started Chat Overflow Framework. Hello everybody!"

    // Initialize chat overflow plugin framework
    init()

    // Add all configured plugins to the plugin registry
    loadPlugins()

    // Testing
    System.exit(0)

    // TODO: Beginning here, a lot has to be made. More input (e.g. arguments), more output (e.g. web interface), ...

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

  def init(): Unit = {
    logger debug "Started init phase."

    // Create plugin framework, registry and manager instance
    val pluginManager = new PluginManagerImpl
    pluginFramework = PluginFramework(pluginFolderPath)
    pluginRegistry = new PluginRegistry(pluginManager)

    // Create sandbox environment for plugins
    Policy.setPolicy(new SandboxSecurityPolicy)
    System.setSecurityManager(new SecurityManager)

    // Initialize plugin framework
    pluginFramework.init(pluginManager)

    // Log infos from loaded plugins
    logger info s"Loaded plugins list:\n\t + ${pluginFramework.getLoadedPlugins.mkString("\n\t + ")}"
    if (pluginFramework.getNotLoadedPlugins.nonEmpty) {
      logger info s"Unable to load:\n\t - ${pluginFramework.getNotLoadedPlugins.mkString("\n\t - ")}"
    }

    // Load Configuration
    configuration = new Configuration(configFolderPath)
    configuration.load()
    logger info s"Loaded configuration from '$configFolderPath'."

    logger debug "Finished init phase."
  }

  def loadPlugins(): Unit = {
    logger info "Loading plugins from config file."

    for (pluginInstance <- configuration.pluginInstances) {

      val pluggable = pluginFramework.getPluggable(PluginId(pluginInstance.pluginName, pluginInstance.pluginAuthor))

      pluggable match {
        case Some(p) =>
          logger info s"Loaded plugin config $pluginInstance."
          pluginRegistry.addPlugin(pluginInstance.instanceName, p)
        case None => logger debug s"Unable to load plugin config $pluginInstance. Plugin not found."
      }
    }

    logger info "Finished loading."
  }
}
