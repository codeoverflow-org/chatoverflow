import java.io.File

import sbt.internal.util.ManagedLogger

/**
  * A build utility instance handles build tasks and prints debug information using the managed logger.
  *
  * @param logger The current logger instance. Usually: {{{streams.value.log}}}
  */
class BuildUtility(logger: ManagedLogger) {

  /**
    * Searches for plugins in plugin directories, builds the plugin build file.
    *
    * @param pluginFolderNames   All folder names, containing plugin source code. Defined in build.sbt.
    * @param pluginBuildFileName The generated sbt build file, containing all sub project references. Defined in build.sbt.
    */
  def fetchPluginsTask(pluginFolderNames: List[String], pluginBuildFileName: String): Unit = {
    withTaskInfo("FETCH PLUGINS") {

      // TODO: Implement

    }
  }

  /**
    * Copies all packaged plugin jars to the target plugin folder.
    *
    * @param pluginFolderNames       All folder names, containing plugin source code. Defined in build.sbt.
    * @param pluginTargetFolderNames The generated sbt build file, containing all sub project references. Defined in build.sbt.
    * @param scalaVersion            The scala version string. Defined in build.sbt.
    */
  def copyPluginsTask(pluginFolderNames: List[String], pluginTargetFolderNames: List[String], scalaVersion: String): Unit = {
    withTaskInfo("COPY PLUGINS") {

      // TODO: Implement

    }
  }

  // Just practising the beauty of scala
  private def withTaskInfo(taskName: String)(task: => Unit): Unit = {

    // Info when task started (better log comprehension)
    logger info s"Started custom task: $taskName"

    // Doing the actual work
    task

    // Info when task stopped (better log comprehension)
    logger info s"Finished custom task: $taskName"
  }

  /**
    * Creates a new plugin. Interactive command using the console.
    *
    * @param pluginFolderNames All folder names, containing plugin source code. Defined in build.sbt.
    */
  def createPluginTask(pluginFolderNames: List[String]): Unit = {
    withTaskInfo("CREATE PLUGIN") {

      // Plugin folders have to be defined in the build.sbt file first
      if (pluginFolderNames.isEmpty) {
        println("Before creating a new plugin, please define at least one plugin source folder in the build.sbt file.")
        logger warn "Aborting task without plugin creation."

      } else {
        println("Welcome to the \"create plugin\"-wizard. Please specify name, version and plugin source folder.")

        // Plugin name
        val name = BuildUtility.askForInput(
          "Please specify the name of the plugin. Do only use characters allowed for directories and files of your OS.",
          "Plugin name",
          repeatIfEmpty = true
        )

        // Plugin version (default: 0.1)
        var version = BuildUtility.askForInput(
          "Please specify the version of the plugin. Just press enter for version \"0.1\".",
          "Plugin version",
          repeatIfEmpty = false
        )
        if (version == "") version = "0.1"

        // Plugin folder name (must be defined in build.sbt)
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
  }

  private def createPlugin(name: String, version: String, pluginFolderName: String): Unit = {
    logger info s"Trying to create plugin $name (version $version) at plugin folder $pluginFolderName."

    val pluginFolder = new File(pluginFolderName)
    if (!pluginFolder.exists()) {
      logger error "Plugin source folder does not exist. Aborting task without plugin creation."

    } else {

      val plugin = new Plugin(pluginFolderName, name)

      if (!plugin.createPluginFolder()) {
        logger error "Plugin does already exist. Aborting task without plugin creation."
      } else {
        logger info s"Created plugin '$name'"

        if (plugin.createSrcFolder()) {
          logger info "Successfully created source folder."
        } else {
          logger warn "Unable to create source folder."
        }

        if (plugin.createSbtFile(version)) {
          logger info "Successfully created plugins sbt file."
        } else {
          logger warn "Unable to create plugins sbt file."
        }

      }
    }
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