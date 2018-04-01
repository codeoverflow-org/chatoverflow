import sbt.internal.util.ManagedLogger

/**
  * A build utility instance handles build tasks and prints debug information using the managed logger.
  *
  * @param logger The current logger instance. Usually: {{{streams.value.log}}}
  */
class BuildUtility(logger: ManagedLogger) {

  /**
    * Creates a new plugin. Interactive command using the console.
    *
    * @param pluginFolderNames All folder names, containing plugin source code. Defined in build.sbt.
    */
  def createPluginTask(pluginFolderNames: List[String]): Unit = {
    logger info "Running custom task: CREATE PLUGIN"

    if (pluginFolderNames.isEmpty) {
      println("Before creating a new plugin, please define at least one plugin source folder in the build.sbt file.")
      logger warn "Stopped task without plugin creation."

    } else {
      println("Welcome to the \"create plugin\"-wizard. Please specify name, version and plugin source folder.")

      val name = BuildUtility.askForInput(
        "Please specify the name of the plugin. Do only use characters allowed for directories and files of your OS.",
        "Plugin name",
        repeatIfEmpty = true
      )

      var version = BuildUtility.askForInput(
        "Please specify the version of the plugin. Just press enter for version \"0.1\".",
        "Plugin version",
        repeatIfEmpty = false
      )
      if (version == "") version = "0.1"

      var pluginFolderName = ""
      while (!pluginFolderNames.contains(pluginFolderName)) {
        pluginFolderName = BuildUtility.askForInput(
          s"Please specify the plugin source directory. Available directories: ${pluginFolderNames.mkString("[", ", ", "]")}",
          "Plugin source directory",
          repeatIfEmpty = true
        )
      }

      createPlugin(name, version, pluginFolderName)
    }
  }

  private def createPlugin(name: String, version: String, pluginFolderName: String): Unit = {
    logger info s"Trying to create plugin $name (version $version) at plugin folder $pluginFolderName."

    // TODO: Implement
  }

  /**
    * Searches for plugins in plugin directories, builds the plugin build file.
    *
    * @param pluginFolderNames   All folder names, containing plugin source code. Defined in build.sbt.
    * @param pluginBuildFileName The generated sbt build file, containing all sub project references. Defined in build.sbt.
    */
  def fetchPluginsTask(pluginFolderNames: List[String], pluginBuildFileName: String): Unit = {
    logger info "Running custom task: FETCH PLUGINS"

  }

  /**
    * Copies all packaged plugin jars to the target plugin folder.
    *
    * @param pluginFolderNames       All folder names, containing plugin source code. Defined in build.sbt.
    * @param pluginTargetFolderNames The generated sbt build file, containing all sub project references. Defined in build.sbt.
    * @param scalaVersion            The scala version string. Defined in build.sbt.
    */
  def copyPluginsTask(pluginFolderNames: List[String], pluginTargetFolderNames: List[String], scalaVersion: String): Unit = {
    logger info "Running custom task: COPY PLUGINS"

  }

}

object BuildUtility {

  def apply(logger: ManagedLogger): BuildUtility = new BuildUtility(logger)

  private def askForInput(information: String, description: String, repeatIfEmpty: Boolean): String = {
    println(information)
    print(s"$description > ")

    var input = scala.io.Source.fromInputStream(System.in).bufferedReader().readLine()
    println("")

    if (input == "" && repeatIfEmpty)
      input = askForInput(information, description, repeatIfEmpty)

    input
  }

}