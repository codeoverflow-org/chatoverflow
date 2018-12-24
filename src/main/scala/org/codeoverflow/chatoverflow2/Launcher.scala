package org.codeoverflow.chatoverflow2

/**
  * The launcher object is the entry point to the chat overflow framework.
  * All UI and framework related tasks are started here.
  */
object Launcher {

  /**
    * Software entry point.
    */
  def main(args: Array[String]): Unit = {
    val chatOverflow = new ChatOverflow
    chatOverflow.init()
  }

}
