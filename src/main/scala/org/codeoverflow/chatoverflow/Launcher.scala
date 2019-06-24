package org.codeoverflow.chatoverflow

import org.codeoverflow.chatoverflow.ui.CLI.parse
import org.codeoverflow.chatoverflow.ui.web.Server

/**
  * The launcher object is the entry point to the chat overflow framework.
  * All UI and framework related tasks are started here.
  */
object Launcher extends WithLogger {

  var server: Option[Server] = None
  var pluginDataPath: String = "data"

  /**
    * Software entry point.
    */
  def main(args: Array[String]): Unit = {
    parse(args) { config =>

      // Set globally available plugin data path
      this.pluginDataPath = config.pluginDataPath

      // The complete project visible trough one single instance
      val chatOverflow = new ChatOverflow(config.pluginFolderPath,
        config.configFolderPath, config.requirementPackage, config.pluginLogOutputOnConsole)

      // Initialize chat overflow
      chatOverflow.init()

      // Login if a password is specified
      if (config.loginPassword.nonEmpty && !chatOverflow.isLoaded) {
        chatOverflow.credentialsService.setPassword(config.loginPassword)
        chatOverflow.load()
      }

      // Launch server (UI backend)
      startServer(chatOverflow, config.webServerPort)

      // Start plugins if specified
      if (config.startupPlugins.nonEmpty) {
        if (chatOverflow.isLoaded) {
          logger info "Starting startup plugins."
          for (instanceName <- config.startupPlugins) {
            val instance = chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName)

            if (instance.isEmpty) {
              logger warn s"No plugin instance named '$instanceName' was registered."
            } else {
              instance.get.start()
            }
          }
        } else {
          logger warn "Unable to run startup plugins. No/wrong password supplied."
        }
      }
    }
  }

  private def startServer(chatOverflow: ChatOverflow, port: Int): Unit = {
    if (server.isEmpty) {
      server = Some(new Server(chatOverflow, port))
      server.get.startAsync()
    }
  }

  /**
    * Shuts down the framework.
    */
  def exit(): Unit = {
    logger info "Shutting down Chat Overflow."
    System.exit(0)
  }
}
