package org.codeoverflow.chatoverflow.ui

import org.codeoverflow.chatoverflow.api.APIVersion

/**
  * The command line interface provides all methods needed to configure the framework and run plugins.
  */
object CLI {

  /**
    * Creating a big parser with all inputs and commands using the SCOPT framework.
    */
  private val argsParser = new scopt.OptionParser[Config]("Chat Overflow") {
    head(s"Chat Overflow ${APIVersion.MAJOR_VERSION}.${APIVersion.MINOR_VERSION}")

    opt[String]('p', "pluginFolder").action((x, c) =>
      c.copy(pluginFolderPath = x)).text("path to a folder with packaged plugin jar files")

    opt[String]('c', "configFolder").action((x, c) =>
      c.copy(configFolderPath = x)).text("path to the folder to save configs and credentials")

    opt[String]('r', "requirementPackage").action((x, c) =>
      c.copy(requirementPackage = x)).text("path to the package where all requirements are defined")

    opt[String]('l', "login").action((x, c) =>
      c.copy(loginPassword = x.toCharArray)).text("the password to login to chat overflow on framework startup")

    opt[Seq[String]]('s', "start").action((x, c) =>
      c.copy(startupPlugins = x)).text("a comma-separated list of plugin instances to start after login (has to be combined with -l)")

    opt[String]('d', "pluginDataFolder").action((x, c) =>
      c.copy(pluginDataPath = x)).text("path to the data folder, accessible from within plugins")

    opt[Int]('w', "webServerPort").action((x, c) =>
      c.copy(webServerPort = x)).text("default web server port, used eg. for the rest api and web gui")

    opt[Unit]('o', "enablePluginOutput").action((_, c) =>
      c.copy(pluginLogOutputOnConsole = true)).text("set this flag to enable plugin log output on console")

    help("help").hidden().text("prints this usage text")

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
  case class Config(pluginFolderPath: String = "plugins/",
                    configFolderPath: String = "config/",
                    requirementPackage: String = "org.codeoverflow.chatoverflow.requirement",
                    pluginDataPath: String = "data",
                    webServerPort: Int = 2400,
                    pluginLogOutputOnConsole: Boolean = false,
                    loginPassword: Array[Char] = Array[Char](),
                    startupPlugins: Seq[String] = Seq[String]())

}

