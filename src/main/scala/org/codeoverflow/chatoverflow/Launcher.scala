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

  /**
    * Software entry point.
    */
  def main(args: Array[String]): Unit = {
    parse(args) { config =>

      // The complete project visible trough one single instance
      val chatOverflow = new ChatOverflow(config.pluginFolderPath,
        config.configFolderPath, config.requirementPackage)

      chatOverflow.init()

      // Launch UI
      config.ui match {
        case UI.GUI =>
          startServer(chatOverflow)
        case UI.REPL =>
          startREPL(chatOverflow)
        case UI.BOTH =>
          startServer(chatOverflow)
          startREPL(chatOverflow)
      }
    }
  }

  private def startServer(chatOverflow: ChatOverflow): Unit = {
    if (server.isEmpty) {
      // TODO: Enable custom port support
      server = Some(new Server(chatOverflow, 2400))
      server.get.startAsync()
    }
  }

  private def startREPL(chatOverflow: ChatOverflow): Unit = {
    new REPL(chatOverflow).run()
  }
}
