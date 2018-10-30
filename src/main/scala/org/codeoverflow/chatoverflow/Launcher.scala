package org.codeoverflow.chatoverflow

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.ui.CLI.{UI, parse}
import org.codeoverflow.chatoverflow.ui.REPL

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

      // TODO: Add GUI code later.

      // Launch UI
      config.ui match {
        case UI.GUI => logger error "GUI is not available yet. Ask dennis."
        case UI.REPL => REPL.run()
      }


    }
  }
}

