import java.io.{File, IOException}
import java.nio.file.{Files, StandardCopyOption}

import BuildUtility._
import sbt.internal.util.ManagedLogger
import sbt.util.{FileFunction, FilesInfo}

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
   * @param pluginTargetFolderNames The name of the directory, in which all plugins should be copied.
   * @param apiProjectPath          The path of the api project. Chosen over apiJarPath if possible.
   * @param apiJarPath              The path of a directory which is containing all api jars. Chosen if apiProjectPath is empty.
   */
  def fetchPluginsTask(pluginSourceFolderNames: List[String], pluginBuildFileName: String,
                       pluginTargetFolderNames: List[String], apiProjectPath: String, apiJarPath: String): Unit = {
    withTaskInfo("FETCH PLUGINS") {

      // Check validity of plugin source folders
      pluginSourceFolderNames.foreach(
        name => if (!Plugin.isSourceFolderNameValid(name, pluginTargetFolderNames))
          logger warn s"Plugin folder '$name' is reserved for build plugin jar files!"
      )

      val allPlugins = getAllPlugins(pluginSourceFolderNames)

      // Create a sbt file with all plugin dependencies (sub projects)
      val sbtFile = new SbtFile("", "", allPlugins, apiProjectPath, apiJarPath, defineRoot = true)

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

      if (installGuiDeps(guiDir, cacheDir).isEmpty)
        return // Early return on failure, error has already been displayed

      val outDir = buildGui(guiDir, cacheDir)
      if (outDir.isEmpty)
        return // Again early return on failure

      // Copy built gui into resources, will be included in the classpath on execution of the framework
      sbt.IO.copyDirectory(outDir.get, new File("src/main/resources/chatoverflow-gui"))
    }
  }

  /**
    * Download the dependencies of the gui using npm.
    *
    * @param guiDir   the directory of the gui.
    * @param cacheDir a dir, where sbt can store files for caching in the "install" sub-dir.
    * @return None, if a error occurs which will be displayed, otherwise the output directory with the built gui.
    */
  private def installGuiDeps(guiDir: File, cacheDir: File): Option[File] = {
    // Check buildGui for a explanation, it's almost the same.

    val install = FileFunction.cached(new File(cacheDir, "install"), FilesInfo.hash)(_ => {

      logger info "Installing GUI dependencies."

      val exitCode = new ProcessBuilder(getNpmCommand :+ "install": _*)
        .inheritIO()
        .directory(guiDir)
        .start()
        .waitFor()

      if (exitCode != 0) {
        logger error "GUI dependencies couldn't be installed, please check above log for further details."
        return None
      } else {
        logger info "GUI dependencies successfully installed."
        Set(new File(guiDir, "node_modules"))
      }
    })

    val input = new File(guiDir, "package.json")
    install(Set(input)).headOption
  }

  /**
    * Builds the gui using npm.
    *
    * @param guiDir   the directory of the gui.
    * @param cacheDir a dir, where sbt can store files for caching in the "build" sub-dir.
    * @return None, if a error occurs which will be displayed, otherwise the output directory with the built gui.
    */
  private def buildGui(guiDir: File, cacheDir: File): Option[File] = {
    // sbt allows easily to cache our external build using FileFunction.cached
    // sbt will only invoke the passed function when at least one of the input files (passed in the last line of this method)
    // has been modified. For the gui these input files are all files in the src directory of the gui and the package.json.
    // sbt passes these input files to the passed function, but they aren't used, we just instruct npm to build the gui.
    // sbt invalidates the cache as well if any of the output files (returned by the passed function) doesn't exist anymore.

    val build = FileFunction.cached(new File(cacheDir, "build"), FilesInfo.hash)(_ => {

      logger info "Building GUI."

      val buildExitCode = new ProcessBuilder(getNpmCommand :+ "run" :+ "build": _*)
        .inheritIO()
        .directory(guiDir)
        .start()
        .waitFor()

      if (buildExitCode != 0) {
        logger error "GUI couldn't be built, please check above log for further details."
        return None
      } else {
        logger info "GUI successfully built."
        Set(new File(guiDir, "dist"))
      }
    })


    val srcDir = new File(guiDir, "src")
    val packageJson = new File(guiDir, "package.json")
    val inputs = recursiveFileListing(srcDir) + packageJson

    build(inputs).headOption
  }

  private def getNpmCommand: List[String] = {
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      List("cmd.exe", "/C", "npm")
    } else {
      List("npm")
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

  /**
   * Creates a file listing with all files including files in any sub-dir.
   *
   * @param f the directory for which the file listing needs to be created.
   * @return the file listing as a set of files.
   */
  def recursiveFileListing(f: File): Set[File] = {
    if (f.isDirectory) {
      f.listFiles().flatMap(recursiveFileListing).toSet
    } else {
      Set(f)
    }
  }
}