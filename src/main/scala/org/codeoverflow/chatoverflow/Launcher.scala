package org.codeoverflow.chatoverflow

import org.codeoverflow.chatoverflow.ui.CLI.{UI, parse}
import org.codeoverflow.chatoverflow.ui.repl.REPL
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
        config.configFolderPath, config.requirementPackage, config.requirePasswordOnStartup, config.pluginLogOutputOnConsole)

      // Initialize chat overflow
      chatOverflow.init()

      // Login if a password is specified
      if (config.loginPassword.nonEmpty && !chatOverflow.isLoaded) {
        chatOverflow.credentialsService.setPassword(config.loginPassword)
        chatOverflow.load()
      }

      // Start plugins if specified
      // TODO: Move this down after server start when the REPL is history
      if (chatOverflow.isLoaded && config.startupPlugins.nonEmpty) {
        for (instanceName <- config.startupPlugins) {
          val instance = chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName)

          if (instance.isEmpty) {
            println(s"No plugin instance named '$instanceName' was registered.")
          } else {
            instance.get.start()
          }
        }
      }

      // Launch UI
      config.ui match {
        case UI.GUI =>
          startServer(chatOverflow, config.webServerPort)
        case UI.REPL =>
          startREPL(chatOverflow)
        case UI.BOTH =>
          startServer(chatOverflow, config.webServerPort)
          startREPL(chatOverflow)
      }
    }
  }

  private def startServer(chatOverflow: ChatOverflow, port: Int): Unit = {
    if (server.isEmpty) {
      server = Some(new Server(chatOverflow, 2400))
      server.get.startAsync()
    }
  }

  private def startREPL(chatOverflow: ChatOverflow): Unit = {
    new REPL(chatOverflow).run()
  }
}
