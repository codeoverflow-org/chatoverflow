import java.io.{File, IOException}
import java.nio.file.Files

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
      pluginSourceFolderNames.foreach(
        name => if (!Plugin.isSourceFolderNameValid(name, pluginTargetFolderNames))
          logger warn s"Plugin folder '$name' is reserved for build plugin jar files!"
      )

      val allPlugins = getAllPlugins(pluginSourceFolderNames)

      // Create a sbt file with all plugin dependencies (sub projects)
      val sbtFile = new SbtFile("", "", allPlugins, apiProjectPath, defineRoot = true)

      if (sbtFile.save(pluginBuildFileName)) {
        logger info s"Successfully updated plugin file at '$pluginBuildFileName'."
      } else {
        logger error s"Unable to write plugin file at '$pluginBuildFileName'."
      }
    }
  }

  private def getAllPlugins(pluginSourceFolderNames: List[String]): List[Plugin] = {

    // Get all plugins (= folders) in all plugin source directories. Flatten that list of lists
    (for (pluginSourceFolderName <- pluginSourceFolderNames) yield {

      logger info s"Fetching plugins from folder '$pluginSourceFolderName'."

      if (!Plugin.sourceFolderExists(pluginSourceFolderName)) {
        logger error s"Plugin directory '$pluginSourceFolderName' does not exist."
        List[Plugin]()

      } else {

        val plugins = Plugin.getPlugins(pluginSourceFolderName)
        logger info s"Found ${plugins.length} plugins."
        plugins
      }
    }).flatten
  }

  /**
    * Copies all packaged plugin jars to the target plugin folder.
    *
    * @param pluginSourceFolderNames All folder names, containing plugin source code. Defined in build.sbt.
    * @param pluginTargetFolderNames The generated sbt build file, containing all sub project references. Defined in build.sbt.
    * @param scalaMajorVersion       The major part (x.x) of the scala version string. Defined in build.sbt.
    */
  def copyPluginsTask(pluginSourceFolderNames: List[String], pluginTargetFolderNames: List[String], scalaMajorVersion: String): Unit = {
    withTaskInfo("COPY PLUGINS") {

      // Get all plugins first
      val allPlugins = getAllPlugins(pluginSourceFolderNames)

      // Now get all jar files in the target folders of the plugins, warn if not found
      val allJarFiles = (for (plugin <- allPlugins) yield {
        val jarFiles = plugin.getBuildPluginFiles(scalaMajorVersion)

        if (jarFiles.isEmpty) {
          logger warn s"Target jar file(s) of plugin '${plugin.name}' does not exist. Use 'sbt package' first."
          Seq[File]()
        } else {
          jarFiles.foreach(jar => logger info s"Found archive: '${jar.getName}'.")
          jarFiles
        }
      }).flatten

      // Copy all jars to the target folders
      for (pluginTargetFolderName <- pluginTargetFolderNames) copyPlugins(allJarFiles, pluginTargetFolderName)
    }
  }

  private def copyPlugins(allJarFiles: List[File], pluginTargetFolderName: String): Unit = {

    val pluginTargetFolder = new File(pluginTargetFolderName)

    // Check target folder existence
    if (!pluginTargetFolder.exists() && !pluginTargetFolder.mkdir()) {
      logger warn s"Unable to create or find plugin folder '${pluginTargetFolder.getAbsolutePath}'."
    } else {
      logger info s"Found plugin folder '${pluginTargetFolder.getPath}'."
    }

    // Clean first
    // TODO: Should this be cleaned? How to handle external plugins? Separate folder?
    for (jarFile <- pluginTargetFolder.listFiles().filter(_.getName.endsWith(".jar"))) {
      try {
        jarFile.delete()
        logger info s"Deleted plugin '${jarFile.getName}' from target."
      } catch {
        case e: IOException => logger warn s"Unable to delete plugin '${jarFile.getAbsolutePath}' from target. Error: ${e.getMessage}."
      }
    }

    // Copy jars
    var successCounter = 0
    for (jarFile <- allJarFiles) {
      try {
        Files.copy(jarFile.toPath, new File(pluginTargetFolder, jarFile.getName).toPath)
        logger info s"Copied plugin '${jarFile.getName}'."
        successCounter = successCounter + 1
      } catch {
        case e: IOException => logger warn s"Unable to copy plugin '${jarFile.getName}'. Error: ${e.getMessage}."
      }
    }
    logger info s"Successfully copied $successCounter / ${allJarFiles.length} plugins to target '${pluginTargetFolder.getPath}'!"
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

  private def withTaskInfo(taskName: String)(task: Unit): Unit = BuildUtility.withTaskInfo(taskName, logger)(task)

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

  /**
    * This method can be used to create better readable sbt console output by declaring start and stop of a custom task.
    *
    * @param taskName the name of the task (use caps for better readability)
    * @param logger   the sbt logger of the task
    * @param task     the task itself
    */
  def withTaskInfo(taskName: String, logger: ManagedLogger)(task: => Unit): Unit = {

    // Info when task started (better log comprehension)
    logger info s"Started custom task: $taskName"

    // Doing the actual work
    task

    // Info when task stopped (better log comprehension)
    logger info s"Finished custom task: $taskName"
  }

}