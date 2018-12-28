package org.codeoverflow.chatoverflow2

import org.codeoverflow.chatoverflow2.ui.CLI.{UI, parse}
import org.codeoverflow.chatoverflow2.ui.repl.REPL

/**
  * The launcher object is the entry point to the chat overflow framework.
  * All UI and framework related tasks are started here.
  */
object Launcher extends WithLogger {

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
        case UI.GUI => logger error "GUI is not available yet. Ask dennis."
        case UI.REPL => new REPL(chatOverflow).run()
      }
    }
  }
}
