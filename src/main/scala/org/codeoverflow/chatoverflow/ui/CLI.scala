package org.codeoverflow.chatoverflow.ui

object CLI {
  private val argsParser = new scopt.OptionParser[Config]("Chat Overflow") {
    head("Chat Overflow")

    opt[String]('p', "pluginFolder").action((x, c) =>
      c.copy(pluginFolderPath = x)).text("path to a folder with packaged plugin jar files")

    opt[String]('c', "configFile").action((x, c) =>
      c.copy(configFilePath = x)).text("path to a custom config xml file")

    opt[String]('d', "credentialsFile").action((x, c) =>
      c.copy(credentialsFilePath = x)).text("path to a custom credentials xml file")

    help("help").hidden().text("prints this usage text")

    cmd("addInstance").action((_, c) => c.copy(mode = "addInstance")).
      text("Adds a plugin instance. Restart required.").
      children(
        opt[String]("instanceName").abbr("i").required().action((x, c) =>
          c.copy(addInstance_instanceName = x)).text("the name of the instance to create"),
        opt[String]("pluginName").abbr("n").required().action((x, c) =>
          c.copy(addInstance_PluginName = x)).text("the name of the plugin to instantiate"),
        opt[String]("pluginAuthor").abbr("a").required().action((x, c) =>
          c.copy(addInstance_PluginAuthor = x)).text("the author of the plugin to instantiate")
      )

    // TODO: addConnector
    // TODO: addCredentials
    // TODO: addRequirement
    // TODO: RunPlugins

    note("\nFor more information, please visit http://codeoverflow.org/")
  }

  def parse(args: Array[String])(code: Config => Unit): Unit = {

    argsParser.parse(args, Config()) match {
      case None => // argument fail
      case Some(config) => code(config)
    }

  }

  private case class Config(pluginFolderPath: String = "", configFilePath: String = "", credentialsFilePath: String = "",
                            mode: String = "",
                            addInstance_instanceName: String = "", addInstance_PluginName: String = "",
                            addInstance_PluginAuthor: String = "",
                            addConnector_type: String = "", addConnector_sourceIdentifier: String = "",
                            addCredentials_type: String = "", addCredentials_sourceIdentifier: String = "",
                            addCredentialsEntry_Key: String = "", addCredentialsEntry_Value: String = "",
                            addRequirement_instanceName: String = "", addRequirement_uniqueId: String = "",
                            addRequirement_targetType: String = "", addRequirement_content: String = "",
                            runPlugins: Seq[String] = Seq())

}

