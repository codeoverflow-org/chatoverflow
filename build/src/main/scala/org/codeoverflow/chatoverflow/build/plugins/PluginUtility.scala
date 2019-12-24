package org.codeoverflow.chatoverflow.build.plugins

import java.io.{File, IOException}
import java.nio.file.{Files, StandardCopyOption}

import org.codeoverflow.chatoverflow.build.{BuildUtils, SbtFile}
import sbt.internal.util.ManagedLogger

/**
 * Handles plugin related build tasks like fetching plugin projects and copying them to the plugin directory.
 *
 * @param logger The current logger instance. Usually: {{{streams.value.log}}}
 */
class PluginUtility(logger: ManagedLogger) {

  /**
   * Searches for plugins in plugin directories, builds the plugin build file.
   *
   * @param pluginSourceFolderNames All folder names, containing plugin source code. Defined in build.sbt.
   * @param pluginBuildFileName     The generated sbt build file, containing all sub project references. Defined in build.sbt.
   * @param pluginTargetFolderNames The name of the directory, in which all plugins should be copied.
   * @param apiProjectPath          The path of the api project.
   * @param guiProjectPath          The path of the gui project.
   */
  def fetchPluginsTask(pluginSourceFolderNames: List[String], pluginBuildFileName: String,
                       pluginTargetFolderNames: List[String], apiProjectPath: String, guiProjectPath: String): Unit = {
    withTaskInfo("FETCH PLUGINS") {

      // Check validity of plugin source folders
      pluginSourceFolderNames.foreach(
        name => if (!Plugin.isSourceFolderNameValid(name, pluginTargetFolderNames))
          logger warn s"Plugin folder '$name' is reserved for build plugin jar files!"
      )

      val allPlugins = getAllPlugins(pluginSourceFolderNames)

      // Create a sbt file with all plugin dependencies (sub projects)
      val sbtFile = new SbtFile("", "", allPlugins, apiProjectPath, guiProjectPath, defineRoot = true)

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


  private def withTaskInfo(taskName: String)(task: Unit): Unit = BuildUtils.withTaskInfo(taskName, logger)(task)
}