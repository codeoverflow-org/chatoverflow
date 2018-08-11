package org.codeoverflow.chatoverflow

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.ui.CLI
import org.codeoverflow.chatoverflow.ui.CLI.{Config, parse}

/**
  * The launcher object is the entry point to the chat overflow framework.
  * All UI and framework related tasks are started here.
  */
object Launcher {

  private val logger = Logger.getLogger(this.getClass)

  /**
    * Software entry point.
    */
  def main(args: Array[String]): Unit = {

    parse(args) { config =>

      // Set custom paths first
      if (config.pluginFolderPath != "")
        ChatOverflow.pluginFolderPath = config.pluginFolderPath

      if (config.credentialsFilePath != "")
        ChatOverflow.credentialsFilePath = config.credentialsFilePath

      if (config.configFilePath != "")
        ChatOverflow.configFilePath = config.configFilePath

      // Init
      ChatOverflow.init()

      // Handle custom commands
      handleCommands(config)

      // TODO: Start web server, web gui, etc.

    }
  }

  private def handleCommands(config: Config): Unit = {
    logger info s"Chat Overflow started with command: '${config.mode}'."

    config.mode match {
      case CLI.modeAddInstance =>
        ChatOverflow.addPluginInstance(config.addInstance_PluginName,
          config.addInstance_PluginAuthor, config.addInstance_instanceName)
      case CLI.modeAddConnector =>
        ChatOverflow.addConnector(config.addConnector_type, config.addConnector_sourceIdentifier)
      case CLI.modeAddCredentials =>
        ChatOverflow.addCredentials(config.addCredentials_type, config.addCredentials_sourceIdentifier)
      case CLI.modeAddCredentialsEntry =>
        ChatOverflow.addCredentialsEntry(config.addCredentialsEntry_type, config.addCredentialsEntry_sourceIdentifier,
          config.addCredentialsEntry_Key, config.addCredentialsEntry_Value)
      case CLI.modeAddRequirement =>
        ChatOverflow.addRequirement(config.addRequirement_instanceName, config.addRequirement_uniqueId,
          config.addRequirement_targetType, config.addRequirement_content)
      case CLI.modeRunPlugins =>
        config.runPlugins.foreach(s => ChatOverflow.startPlugin(s))
      case _ =>
        logger info "Doing nothing. Much wow!"
    }
  }
}

