package org.codeoverflow.chatoverflow.ui.repl

import org.codeoverflow.chatoverflow.ChatOverflow
import org.codeoverflow.chatoverflow.configuration.{ConfigurationService, Credentials}
import org.codeoverflow.chatoverflow.connector.ConnectorRegistry

/**
  * The REPL is used to control the chat overflow environment from the console.
  *
  * @param chatOverflow the chat overflow object to operate on
  */
class REPL(chatOverflow: ChatOverflow) {

  private val commands = Map(
    "help" -> REPLCommand(_ => help(), "Prints all available commands."),
    "instance" -> REPLCommand(_ => addInstance(), "Adds a new plugin instance."),
    "list" -> REPLCommand(_ => listInstances(), "Lists all registered plugin instances."),
    "connector" -> REPLCommand(_ => addConnector(), "Adds a new connector."),
    "credentials" -> REPLCommand(_ => addCredentials(), "Adds a credentials placeholder for a connector."),
    "entry" -> REPLCommand(_ => addCredentialsEntry(), "Adds a credentials entry to a existing placeholder."),
    "requirement" -> REPLCommand(_ => addRequirement(), "Adds a requirement to a already existing plugin instance."),
    "run" -> REPLCommand(_ => runPlugin(), "Starts a plugin instance. Make sure to configure everything other first!"),
    "start" -> REPLCommand(_ => runPlugin(), "Starts a plugin instance. Make sure to configure everything other first!"),
    "save" -> REPLCommand(_ => save(), "Saves the current state (credentials and configs) of the framework."),
    "exit" -> REPLCommand(_ => exit(), "Quits the chat overflow framework."),
    "quit" -> REPLCommand(_ => exit(), "Quits the chat overflow framework, too!")
  )

  /**
    * Start the REPL, receiving input until exit/quit is entered.
    */
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

    val pluginType = chatOverflow.pluginFramework.getPlugin(pluginName, pluginAuthor)
    if (pluginType.isEmpty) {
      println(s"Plugin type with name '$pluginName' and author '$pluginAuthor' not found.")
    } else {
      chatOverflow.pluginInstanceRegistry.addPluginInstance(instanceName, pluginType.get)
    }
  }

  private def addConnector(): Unit = {
    println("Please provide full connector type string and connector id.")

    val connectorType = scala.io.StdIn.readLine("Connector Type String >")
    val connectorId = scala.io.StdIn.readLine("Connector ID >")

    ConnectorRegistry.addConnector(connectorId, connectorType)
  }

  private def addCredentials(): Unit = {
    println("Please provide credentials type string (same as connector) and credentials id (same as connector).")

    val credentialsType = scala.io.StdIn.readLine("Credentials (Connector) Type String >")
    val credentialsId = scala.io.StdIn.readLine("Credentials (Connector) ID >")

    val credentials = new Credentials(credentialsId)
    ConnectorRegistry.setConnectorCredentials(credentialsId, credentialsType, credentials)
  }

  private def addCredentialsEntry(): Unit = {
    println("Please provide credentials type, id and credentials entry key and value.")

    val credentialsType = scala.io.StdIn.readLine("Credentials Type String >")
    val credentialsId = scala.io.StdIn.readLine("Credentials ID >")
    val credentialsKey = scala.io.StdIn.readLine("Credentials Entry Key >")
    val credentialsValue = scala.io.StdIn.readLine("Credentials Entry Value >")

    val connector = ConnectorRegistry.getConnector(credentialsId, credentialsType)
    if (connector.isEmpty) {
      println(s"Connector of type '$credentialsType' with id '$credentialsId' not found.")
    } else {
      if (connector.get.setCredentialsValue(credentialsKey, credentialsValue)) {
        println(s"Setting credentials entry with key '$credentialsKey' was successful.")
      } else {
        println("Unable to set credentials. Connector or credentials were not created first.")
      }
    }
  }

  private def addRequirement(): Unit = {
    println("Please provide plugin instance name, requirementId, requirement target type and serialized value.")

    val instanceName = scala.io.StdIn.readLine("Instance Name >")
    val requirementId = scala.io.StdIn.readLine("Requirement ID >")
    val targetType = scala.io.StdIn.readLine("Target Type String >")
    val content = scala.io.StdIn.readLine("Serialized Content >")

    ConfigurationService.fulfillRequirementByDeserializing(instanceName,
      requirementId, targetType, content, chatOverflow.pluginInstanceRegistry, chatOverflow.typeRegistry)
  }

  private def runPlugin(): Unit = {
    println("Please provide the name of the plugin instance to run.")

    val instanceName = scala.io.StdIn.readLine("Instance Name >")

    val instance = chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName)

    if (instance.isEmpty) {
      println(s"No plugin instance named '$instanceName' was registered.")
    } else {
      instance.get.start()
    }
  }

  private def save(): Unit = {
    chatOverflow.save()
  }

  private def listInstances(): Unit = {
    println(chatOverflow.pluginInstanceRegistry.getAllPluginInstances
      .map(inst => inst.instanceName).mkString(", "))
  }

  // TODO: Enable shutting down everything correctly by function call
  // TODO: Proper repl ask function... i mean... why?
  // TODO: Add more functionality

  private def help(): Unit = {
    println("Available commands:\n")
    commands.foreach(cmd => println(s"${cmd._1}:\t${cmd._2.description}"))
  }

  private def exit(): Unit = {
    System.exit(0)
  }
}

