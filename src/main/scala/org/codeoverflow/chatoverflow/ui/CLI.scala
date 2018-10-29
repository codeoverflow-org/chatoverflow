package org.codeoverflow.chatoverflow.ui

/**
  * The command line interface provides all methods needed to configure the framework and run plugins.
  */
object CLI {

  // TODO: Replace CLI with REPL

  val modeAddInstance = "addInstance"
  val modeAddConnector = "addConnector"
  val modeAddCredentials = "addCredentials"
  val modeAddCredentialsEntry = "addCredentialsEntry"
  val modeAddRequirement = "addRequirement"
  val modeRunPlugins = "runPlugins"

  /**
    * Creating a big parser with all inputs and commands using the SCOPT framework.
    */
  private val argsParser = new scopt.OptionParser[Config]("Chat Overflow") {
    head("Chat Overflow")

    opt[String]('p', "pluginFolder").action((x, c) =>
      c.copy(pluginFolderPath = x)).text("path to a folder with packaged plugin jar files")

    opt[String]('c', "configFile").action((x, c) =>
      c.copy(configFilePath = x)).text("path to a custom config xml file")

    opt[String]('d', "credentialsFile").action((x, c) =>
      c.copy(credentialsFilePath = x)).text("path to a custom credentials xml file")

    help("help").hidden().text("prints this usage text")

    note("\n")

    cmd(modeAddInstance).action((_, c) => c.copy(mode = modeAddInstance)).
      text("Adds a plugin instance. Restart required.").
      children(
        opt[String]("instanceName").abbr("i").required().action((x, c) =>
          c.copy(addInstance_instanceName = x)).text("the name of the instance to create"),
        opt[String]("pluginName").abbr("n").required().action((x, c) =>
          c.copy(addInstance_PluginName = x)).text("the name of the plugin to instantiate"),
        opt[String]("pluginAuthor").abbr("a").required().action((x, c) =>
          c.copy(addInstance_PluginAuthor = x)).text("the author of the plugin to instantiate")
      )

    note("\n")

    cmd(modeAddConnector).action((_, c) => c.copy(mode = modeAddConnector)).
      text("Adds a connector. Restart required.").
      children(
        opt[String]("connectorType").abbr("t").required().action((x, c) =>
          c.copy(addConnector_type = x)).text("the type string of the connector (from the api)"),
        opt[String]("connectorId").abbr("i").required().action((x, c) =>
          c.copy(addConnector_sourceIdentifier = x)).text("the source name for the connection"),
      )

    note("\n")

    cmd(modeAddCredentials).action((_, c) => c.copy(mode = modeAddCredentials)).
      text("Adds a credentials placeholder. Will probably need entries to work correctly.").
      children(
        opt[String]("credentialsType").abbr("t").required().action((x, c) =>
          c.copy(addCredentials_type = x)).text("the type string of the credentials (same as the connector)"),
        opt[String]("credentialsId").abbr("i").required().action((x, c) =>
          c.copy(addCredentials_sourceIdentifier = x)).text("the source id of the credentials (same as the connector)"),
      )

    note("\n")

    cmd(modeAddCredentialsEntry).action((_, c) => c.copy(mode = modeAddCredentialsEntry)).
      text("Adds a credentials entry to a existing placeholder. Restart required.").
      children(
        opt[String]("credentialsType").abbr("t").required().action((x, c) =>
          c.copy(addCredentialsEntry_type = x)).text("the type string of the credentials (same as the connector)"),
        opt[String]("credentialsId").abbr("i").required().action((x, c) =>
          c.copy(addCredentialsEntry_sourceIdentifier = x)).text("the source id of the credentials (same as the connector)"),
        opt[String]("credentialsKey").abbr("k").required().action((x, c) =>
          c.copy(addCredentialsEntry_Key = x)).text("specifies the key of the credentials entry"),
        opt[String]("credentialsValue").abbr("v").required().action((x, c) =>
          c.copy(addCredentialsEntry_Value = x)).text("the value to set in the registered credentials")
      )

    note("\n")

    cmd(modeAddRequirement).action((_, c) => c.copy(mode = modeAddRequirement)).
      text("Adds a requirement to a already existing plugin instance. Restart required.").
      children(
        opt[String]("instanceName").abbr("i").required().action((x, c) =>
          c.copy(addRequirement_instanceName = x)).text("the name of the instance to set a requirement"),
        opt[String]("requirementId").abbr("k").required().action((x, c) =>
          c.copy(addRequirement_uniqueId = x)).text("the id of the requirement. has to be plugin unique"),
        opt[String]("targetType").abbr("t").required().action((x, c) =>
          c.copy(addRequirement_targetType = x)).text("the target type of the requirement, e.g. which source"),
        opt[String]("content").abbr("c").required().action((x, c) =>
          c.copy(addRequirement_content = x)).text("the serialized content of that requirement")
      )

    note("\n")

    cmd(modeRunPlugins).action((_, c) => c.copy(mode = modeRunPlugins)).
      text("Run the specified, command separated plugin instances. Remember to add connectors, credentials and requirements first.").
      children(
        opt[Seq[String]]("plugins").abbr("p").required().action((x, c) =>
          c.copy(runPlugins = x)).text("the instance names of all plugins, comma separated"),
      )

    note("\n")

    note("\nFor more information, please visit http://codeoverflow.org/")
  }

  /**
    * This method takes all command line args and fills the information into the Config case class.
    *
    * @param args a plain command line args from the vm
    * @param code this code is executed when all args had been parsed
    */
  def parse(args: Array[String])(code: Config => Unit): Unit = {

    argsParser.parse(args, Config()) match {
      case None => // argument fail
      case Some(config) => code(config)
    }

  }

  /**
    * This case class holds all information that can be set when starting the framework from command line.
    */
  case class Config(pluginFolderPath: String = "", configFilePath: String = "", credentialsFilePath: String = "",
                    mode: String = "",
                    addInstance_instanceName: String = "", addInstance_PluginName: String = "",
                    addInstance_PluginAuthor: String = "",
                    addConnector_type: String = "", addConnector_sourceIdentifier: String = "",
                    addCredentials_type: String = "", addCredentials_sourceIdentifier: String = "",
                    addCredentialsEntry_sourceIdentifier: String = "", addCredentialsEntry_type: String = "",
                    addCredentialsEntry_Key: String = "", addCredentialsEntry_Value: String = "",
                    addRequirement_instanceName: String = "", addRequirement_uniqueId: String = "",
                    addRequirement_targetType: String = "", addRequirement_content: String = "",
                    runPlugins: Seq[String] = Seq())

}

