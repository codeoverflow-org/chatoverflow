package org.codeoverflow.chatoverflow

import org.apache.log4j.Logger

object Launcher {

  private val logger = Logger.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    ChatOverflow.init()
    ChatOverflow.startPlugin("myfirstinstance")
  }

}
