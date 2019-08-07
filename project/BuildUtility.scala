import java.io.{File, IOException}
import java.nio.file.{Files, StandardCopyOption}
import java.util.jar.Manifest

import com.fasterxml.jackson.databind.ObjectMapper
import sbt.internal.util.ManagedLogger
import sbt.util.{FileFunction, FilesInfo}

import scala.io.Source

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
  *       |  -> gui project
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

    // Copy jars
    var successCounter = 0
    for (jarFile <- allJarFiles) {
      try {
        Files.copy(jarFile.toPath, new File(pluginTargetFolder, jarFile.getName).toPath, StandardCopyOption.REPLACE_EXISTING)
        logger info s"Copied plugin '${jarFile.getName}'."
        successCounter = successCounter + 1
      } catch {
        case e: IOException => logger warn s"Unable to copy plugin '${jarFile.getName}'. Error: ${e.getMessage}."
      }
    }
    logger info s"Successfully copied $successCounter / ${allJarFiles.length} plugins to target '${pluginTargetFolder.getPath}'!"
  }

  def guiTask(guiProjectPath: String, cacheDir: File): Unit = {
    withTaskInfo("BUILD GUI") {
      val guiDir = new File(guiProjectPath)
      if (!guiDir.exists()) {
        logger warn s"GUI not found at $guiProjectPath, ignoring GUI build."
        return
      }

      val packageJson = new File(guiDir, "package.json")

      if (!executeNpmCommand(guiDir, cacheDir, Set(packageJson), "install",
        () => logger error "GUI dependencies couldn't be installed, please check above log for further details.",
        () => new File(guiDir, "node_modules")
      )) {
        return // early return on failure, error has already been displayed
      }

      val srcFiles = recursiveFileListing(new File(guiDir, "src"))
      val outDir = new File(guiDir, "dist")

      executeNpmCommand(guiDir, cacheDir, srcFiles + packageJson, "run build",
        () => logger error "GUI couldn't be built, please check above log for further details.",
        () => outDir
      )
    }
  }

  /**
   * Executes a npm command in the given directory and skips executing the given command
   * if no input files have changed and the output file still exists.
   *
   * @param workDir  the directory in which npm should be executed
   * @param cacheDir a directory required for caching using sbt
   * @param inputs   the input files, which will be used for caching.
   *                 If any one of these files change the cache is invalidated.
   * @param command  the npm command to execute
   * @param failed   called if npm returned an non-zero exit code
   * @param success  called if npm returned successfully. Needs to return a file for caching.
   *                 If the returned file doesn't exist the npm command will ignore the cache.
   * @return true if npm returned zero as a exit code and false otherwise
   */
  private def executeNpmCommand(workDir: File, cacheDir: File, inputs: Set[File], command: String,
                                failed: () => Unit, success: () => File): Boolean = {
    // sbt allows easily to cache our external build using FileFunction.cached
    // sbt will only invoke the passed function when at least one of the input files (passed in the last line of this method)
    // has been modified. For the gui these input files are all files in the src directory of the gui and the package.json.
    // sbt passes these input files to the passed function, but they aren't used, we just instruct npm to build the gui.
    // sbt invalidates the cache as well if any of the output files (returned by the passed function) doesn't exist anymore.
    val cachedFn = FileFunction.cached(new File(cacheDir, command), FilesInfo.hash) { _ => {
      val exitCode = new ProcessBuilder(getNpmCommand ++ command.split("\\s+"): _*)
        .inheritIO()
        .directory(workDir)
        .start()
        .waitFor()

      if (exitCode != 0) {
        failed()
        return false
      } else {
        Set(success())
      }
    }
    }

    cachedFn(inputs)
    true
  }

  private def getNpmCommand: List[String] = {
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      List("cmd.exe", "/C", "npm")
    } else {
      List("npm")
    }
  }

  def packageGUITask(guiProjectPath: String, scalaMajorVersion: String, crossTargetDir: File): Unit = {
    val dir = new File(guiProjectPath, "dist")
    if (!dir.exists()) {
      logger info "GUI hasn't been compiled. Won't create a jar for it."
      return
    }

    val files = recursiveFileListing(dir)

    // contains tuples with the actual file as the first value and the name with directory in the jar as the second value
    val jarEntries = files.map(file => file -> s"/chatoverflow-gui/${dir.toURI.relativize(file.toURI).toString}")

    val guiVersion = getGUIVersion(guiProjectPath).getOrElse("unknown")

    sbt.IO.jar(jarEntries, new File(crossTargetDir, s"chatoverflow-gui_$scalaMajorVersion-$guiVersion.jar"), new Manifest())
  }

  private def getGUIVersion(guiProjectPath: String): Option[String] = {
    val packageJson = new File(s"$guiProjectPath/package.json")
    if (!packageJson.exists()) {
      logger error "The package.json file of the GUI doesn't exist. Have you cloned the GUI in the correct directory?"
      return None
    }

    val content = Source.fromFile(packageJson)
    val version = new ObjectMapper().reader().readTree(content.mkString).get("version").asText()

    content.close()

    if (version.isEmpty) {
      logger warn "The GUI version couldn't be loaded from the package.json."
      None
    } else {
      Option(version)
    }
  }

  /**
    * Creates a file listing with all files including files in any sub-dir.
    *
    * @param f the directory for which the file listing needs to be created.
    * @return the file listing as a set of files.
    */
  private def recursiveFileListing(f: File): Set[File] = {
    if (f.isDirectory) {
      f.listFiles().flatMap(recursiveFileListing).toSet
    } else {
      Set(f)
    }
  }

  private def withTaskInfo(taskName: String)(task: Unit): Unit = BuildUtility.withTaskInfo(taskName, logger)(task)
}

object BuildUtility {

  def apply(logger: ManagedLogger): BuildUtility = new BuildUtility(logger)

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