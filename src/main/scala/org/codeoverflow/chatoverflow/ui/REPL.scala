package org.codeoverflow.chatoverflow.ui

import org.apache.log4j.Logger

object REPL {

  private val logger = Logger.getLogger(this.getClass)
  private val commands = Map(
    "help" -> REPLCommand(_ => help(), "Prints all available commands."),
    "exit" -> REPLCommand(_ => exit(), "Quits the chat overflow framework."),
    "quit" -> REPLCommand(_ => exit(), "Quits the chat overflow framework, too!")
  )

  def run(): Unit = {
    println("Welcome to the Chat Overflow REPL!\n" +
      s"Type 'help' to get information about available commands.")

    while (true) {
      val userInput = scala.io.StdIn.readLine("> ")

      if (commands.contains(userInput)) {
        commands(userInput).methodToCall()
      } else {
        println("Unrecognized command. Please try again.")
      }
    }

  }

  private def help(): Unit = {
    println("Available commands:\n")
    commands.foreach(cmd => println(s"${cmd._1}:\t${cmd._2.description}"))
  }

  private def exit(): Unit = {
    System.exit(0)
  }
}

case class REPLCommand(methodToCall: Unit => Unit, description: String)
