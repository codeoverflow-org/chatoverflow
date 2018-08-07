package org.codeoverflow.chatoverflow

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.ui.CLI
import org.codeoverflow.chatoverflow.ui.CLI.{Config, parse}

object Launcher {

  private val logger = Logger.getLogger(this.getClass)

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

  def handleCommands(config: Config): Unit = {
    logger info s"Chat Overflow started with command: '${config.mode}'."

    config.mode match {
      case CLI.modeAddInstance =>
      case CLI.modeAddConnector =>
      case CLI.modeAddCredentials =>
      case CLI.modeAddCredentialsEntry =>
      case CLI.modeAddRequirement =>
      case CLI.modeRunPlugins =>
    }
  }
}

