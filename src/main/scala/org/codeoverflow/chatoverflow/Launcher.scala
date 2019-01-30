package org.codeoverflow.chatoverflow

import org.codeoverflow.chatoverflow.ui.CLI.{UI, parse}
import org.codeoverflow.chatoverflow.ui.repl.REPL
import org.codeoverflow.chatoverflow.ui.web.Server

/**
  * The launcher object is the entry point to the chat overflow framework.
  * All UI and framework related tasks are started here.
  */
object Launcher extends WithLogger {

  var chatOverflow: Option[ChatOverflow] = None

  /**
    * Software entry point.
    */
  def main(args: Array[String]): Unit = {
    parse(args) { config =>

      // The complete project visible trough one single instance
      chatOverflow = Some(new ChatOverflow(config.pluginFolderPath,
        config.configFolderPath, config.requirementPackage))

      chatOverflow.get.init()

      new Server(12345).startAsync()
      new REPL().run()
    }
  }
}
