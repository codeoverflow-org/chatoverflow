import java.io.File

import sbt.internal.util.ManagedLogger

/**
  * A build utility instance handles build tasks and prints debug information using the managed logger.
  *
  * @param logger The current logger instance. Usually: {{{streams.value.log}}}
  * @note A brief introduction of the folder structure:
  *       root
  *       |  build.sbt
  *       |  plugins.sbt
  *       |  -> api project
  *       |  -> a plugin source directory
  *       |  -> -> a plugin folder = plugin
  *       |  -> -> -> build.sbt
  *       |  -> -> -> source etc.
  *       |  -> -> another folder = another plugin
  *       |  -> -> -> build.sbt
  *       |  -> -> -> source etc.
  *       |  -> another plugin source directory (optional)
  *
  */
class BuildUtility(logger: ManagedLogger) {

  /**
    * Searches for plugins in plugin directories, builds the plugin build file.
    *
    * @param pluginSourceFolderNames All folder names, containing plugin source code. Defined in build.sbt.
    * @param pluginBuildFileName     The generated sbt build file, containing all sub project references. Defined in build.sbt.
    */
  def fetchPluginsTask(pluginSourceFolderNames: List[String], pluginBuildFileName: String,
                       pluginTargetFolderNames: List[String], apiProjectPath: String): Unit = {
    withTaskInfo("FETCH PLUGINS") {

      // Check validity of plugin source folders
      pluginSourceFolderNames.foreach(name => checkSourceFolderName(name, pluginTargetFolderNames))

      // Get all plugins (= folders) in all plugin source directories. Flatten that list of lists
      val allPlugins = (for (pluginSourceFolderName <- pluginSourceFolderNames) yield getPlugins(pluginSourceFolderName)).flatten

      // Create a sbt file with all plugin dependencies (sub projects)
      val sbtFile = new SbtFile("", "", allPlugins, apiProjectPath, defineRoot = true)

      if (sbtFile.save(pluginBuildFileName)) {
        logger info s"Successfully updated plugin file at '$pluginBuildFileName'."
      } else {
        logger error s"Unable to write plugin file at '$pluginBuildFileName'."
      }
    }
  }

  private def checkSourceFolderName(pluginSourceFolderName: String, pluginTargetFolderNames: List[String]): Unit = {
    if (pluginTargetFolderNames.map(_.toLowerCase).contains(pluginSourceFolderName.toLowerCase)) {
      logger warn s"Plugin folder '$pluginSourceFolderName' is reserved for build plugin jar files!"
    }
  }

  private def getPlugins(pluginSourceFolderName: String): Seq[Plugin] = {
    logger info s"Fetching plugins from folder '$pluginSourceFolderName'."

    val pluginSourceFolder = new File(pluginSourceFolderName)

    // Check for invalid folder structure
    if (!pluginSourceFolder.exists() || !pluginSourceFolder.isDirectory) {
      logger error s"Plugin directory '$pluginSourceFolderName' does not exist."
      Seq[Plugin]()
    } else {

      // A plugin is a folder in a plugin directory
      val plugins = pluginSourceFolder.listFiles.filter(_.isDirectory)
        .map(folder => new Plugin(pluginSourceFolderName, folder.getName))

      logger info s"Found ${plugins.length} plugins."
      plugins
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