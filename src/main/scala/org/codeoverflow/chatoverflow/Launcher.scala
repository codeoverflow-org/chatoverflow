package org.codeoverflow.chatoverflow

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.ui.CLI.parse

object Launcher {

  private val logger = Logger.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {

    parse(args) { config =>
      ChatOverflow.init()

      // TODO: React!

    }
  }
}

