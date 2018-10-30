package org.codeoverflow.chatoverflow.ui

import org.codeoverflow.chatoverflow.ChatOverflow

object REPL {

  private val commands = Map(
    "help" -> REPLCommand(_ => help(), "Prints all available commands."),
    "instance" -> REPLCommand(_ => addInstance(), "Adds a new plugin instance. RESTART REQUIRED."),
    "connector" -> REPLCommand(_ => addConnector(), "Adds a new connector. RESTART REQUIRED."),
    "credentials" -> REPLCommand(_ => addCredentials(), "Adds a credentials placeholder for a connector."),
    "entry" -> REPLCommand(_ => addCredentialsEntry(), "Adds a credentials entry to a existing placeholder. RESTART REQUIRED."),
    "requirement" -> REPLCommand(_ => addRequirement(), "Adds a requirement to a already existing plugin instance. RESTART REQUIRED."),
    "run" -> REPLCommand(_ => runPlugin(), "Runs a plugin. Make sure to configure everything other first!"),
    "start" -> REPLCommand(_ => runPlugin(), "Runs a plugin. Make sure to configure everything other first!"),
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

  private def addInstance(): Unit = {
    println("Please provide plugin name, plugin author and instance name.")

    val pluginName = scala.io.StdIn.readLine("Plugin Name >")
    val pluginAuthor = scala.io.StdIn.readLine("Plugin Author >")
    val instanceName = scala.io.StdIn.readLine("Instance Name >")

    ChatOverflow.addPluginInstance(pluginName, pluginAuthor, instanceName)
  }

  private def addConnector(): Unit = {
    println("Please provide full connector type string and connector id.")

    val connectorType = scala.io.StdIn.readLine("Connector Type String >")
    val connectorId = scala.io.StdIn.readLine("Connector ID >")

    ChatOverflow.addConnector(connectorType, connectorId)
  }

  private def addCredentials(): Unit = {
    println("Please provide credentials type string (same as connector) and credentials id (same as connector).")

    val credentialsType = scala.io.StdIn.readLine("Credentials (Connector) Type String >")
    val credentialsId = scala.io.StdIn.readLine("Credentials (Connector) ID >")

    ChatOverflow.addCredentials(credentialsType, credentialsId)
  }

  private def addCredentialsEntry(): Unit = {
    println("Please provide credentials type, id and credentials entry key and value. RESTART REQUIRED.")

    val credentialsType = scala.io.StdIn.readLine("Credentials Type String >")
    val credentialsId = scala.io.StdIn.readLine("Credentials ID >")
    val credentialsKey = scala.io.StdIn.readLine("Credentials Entry Key >")
    val credentialsValue = scala.io.StdIn.readLine("Credentials Entry Value >")

    ChatOverflow.addCredentialsEntry(credentialsType, credentialsId, credentialsKey, credentialsValue)
  }

  private def addRequirement(): Unit = {
    println("Please provide plugin instance name, requirementId, requirement target type and serialized value. RESTART REQUIRED.")

    val instanceName = scala.io.StdIn.readLine("Instance Name >")
    val requirementId = scala.io.StdIn.readLine("Requirement ID >")
    val targetType = scala.io.StdIn.readLine("Target Type String >")
    val content = scala.io.StdIn.readLine("Serialized Content >")

    ChatOverflow.addRequirement(instanceName, requirementId, targetType, content)
  }

  private def runPlugin(): Unit = {
    println("Please provide the name of the plugin instance to run.")

    val instanceName = scala.io.StdIn.readLine("Instance Name >")

    ChatOverflow.startPlugin(instanceName)
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