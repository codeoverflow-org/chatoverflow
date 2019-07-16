import java.io.File

import BuildUtility._
import PluginCreateWizard._
import sbt.internal.util.ManagedLogger


class PluginCreateWizard(logger: ManagedLogger) {
  
  /**
    * Creates a new plugin. Interactive command using the console.
    *
    * @param pluginFolderNames All folder names, containing plugin source code. Defined in build.sbt.
    */
  def createPluginTask(pluginFolderNames: List[String]): Unit = {
    withTaskInfo("CREATE PLUGIN", logger) {

      // Plugin folders have to be defined in the build.sbt file first
      if (pluginFolderNames.isEmpty) {
        println("Before creating a new plugin, please define at least one plugin source folder in the build.sbt file.")
        logger warn "Aborting task without plugin creation."
        return
      }

      println("Welcome to the \"create plugin\"-wizard. Please answer all the following questions to automatically create a plugin.")

      // Plugin name
      val name = askForInput(
        "Please specify the name of the plugin. Do only use characters allowed for directories and files of your OS.",
        "Plugin name",
        s => s.nonEmpty
      )

      val author = askForInput(
        "Please specify the name of the author of the plugin. Can be a real name or alias.",
        "Author name",
        s => s.nonEmpty
      )

      // Plugin version (default: 0.1)
      val version = askForInput(
        "Please specify the version of the plugin. Just press enter for version \"0.1\".",
        "Plugin version", "0.1"
      )

      // Plugin folder name (must be defined in build.sbt)
      val pluginFolderName = askForInput(
        s"Please specify the plugin source directory. Available directories: ${pluginFolderNames.mkString("[", ", ", "]")}",
        "Plugin source directory",
        s => s.nonEmpty && pluginFolderNames.contains(s)
      )

      // Plugin metadata
      val description = askForInput("Please specify a optional description of what this plugin does.", "Description")
      val licence = askForInput("Please specify the SPDX short identifier of the used license, if any e.g. 'MIT' or 'EPL-2.0'.", "License")
      val website = askForInput("Please specify a optional website about you or the plugin", "Website")
      val sourceRepo = askForInput("The repository where the code of this plugin is hosted, if published.", "Source repo")
      val bugtracker = askForInput("A optional bug tracker or other support website.", "Bug tracker")

      val metadata = PluginMetadata(description, licence, website, sourceRepo, bugtracker)

      createPlugin(name, author, version, pluginFolderName, metadata)
    }
  }

  private def createPlugin(name: String, author: String, version: String, pluginFolderName: String, metadata: PluginMetadata): Unit = {
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

        if (plugin.createSrcFolders()) {
          logger info "Successfully created source folder."

          if (plugin.createPluginXMLFile(metadata, author, version)) {
            logger info "Successfully created plugin.xml containing metadata."
          } else {
            logger warn "Unable to create plugin.xml containing metadata in plugin resources."
          }
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

object PluginCreateWizard {

  def apply(logger: ManagedLogger): PluginCreateWizard = new PluginCreateWizard(logger)

  private def askForInput(information: String, description: String, validate: String => Boolean): String = {
    println(information)
    print(s"$description > ")

    val input = scala.io.Source.fromInputStream(System.in).bufferedReader().readLine()
    println("")

    if (validate(input))
      input
    else
      askForInput(information, description, validate)
  }

  private def askForInput(information: String, description: String): String = askForInput(information, description, _ => true)

  private def askForInput(information: String, description: String, default: String): String = {
    val input = askForInput(information, description, _ => true)

    if (input.isEmpty) default
    else input
  }
}