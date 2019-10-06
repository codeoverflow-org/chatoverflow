package org.codeoverflow.chatoverflow.build.plugins

import java.io.File

import org.codeoverflow.chatoverflow.build.BuildUtils
import org.codeoverflow.chatoverflow.build.BuildUtils.withTaskInfo
import org.codeoverflow.chatoverflow.build.plugins.PluginCreateWizard.askForInput
import sbt.Keys._
import sbt.internal.util.ManagedLogger
import sbt.{Def, Task}

import scala.annotation.tailrec
import scala.util.Try

class PluginCreateWizard(logger: ManagedLogger) {

  /**
   * Creates a new plugin. Interactive command using the console.
   *
   * @param pluginFolderNames All folder names, containing plugin source code. Defined in build.sbt.
   */
  def createPluginTask(pluginFolderNames: List[String], apiVersion: Option[(Int, Int)]): Unit = {
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
        s => s.nonEmpty, "Plugin name mustn't be empty"
      )

      val author = askForInput(
        "Please specify the name of the author of the plugin. Can be a real name or alias.",
        "Author name",
        s => s.nonEmpty, "Plugin author mustn't be empty"
      )

      // Plugin version (default: 0.1)
      val version = askForInput(
        "Please specify the version of the plugin. Just press enter for version \"0.1\".",
        "Plugin version", "0.1"
      )

      // Plugin folder name (must be defined in build.sbt)
      val pluginFolderName = askForInput(
        s"Please specify the plugin source directory. Available directories: ${pluginFolderNames.mkString("[", ", ", "]")}",
        "Plugin source directory", s => s.nonEmpty && pluginFolderNames.contains(s),
        "Source directory must be one of the provided directories."
      )

      val availableLanguages = PluginLanguage.values.mkString(", ").toLowerCase
      val languageString = askForInput(
        "Please specify the programming language, in which this project is going to be coded. " +
          s"Available languages: $availableLanguages.",
        "Plugin language",
        s => PluginLanguage.fromString(s).isDefined,
        s"The language must be one of the following: $availableLanguages"
      )
      val language = PluginLanguage.fromString(languageString).get // guaranteed to not be empty by the validate function above

      // In case we couldn't figure out the api version, maybe because the api project didn't exist, we ask the user for it.
      val api = {
        // not empty and must be a valid number, can be skipped if a default is available
        val validate = (s: String) => s.nonEmpty && s.forall(_.isDigit) || apiVersion.isDefined
        val major = askForInput("Please specify the major version of the api. " +
          (if (apiVersion.isEmpty) "Check api/build.sbt for it."
          else s"Default is the current version (${apiVersion.get._1})."),
          "Major api version", validate, "Api version must be a number")
        val minor = askForInput(s"Please specify the minor version of the api. " +
          (if (apiVersion.isEmpty) "Check api/build.sbt for it."
          else s"Default is the current version (${apiVersion.get._2})."),
          "Minor api version", validate, "Api version must be a number")

        (if (major.isEmpty) apiVersion.get._1 else major.toInt, if (minor.isEmpty) apiVersion.get._2 else minor.toInt)
      }

      // Plugin metadata
      val description = askForInput("Please specify a optional description of what this plugin does.", "Description")
      val licence = askForInput("Please specify the SPDX short identifier of the used license, if any e.g. 'MIT' or 'EPL-2.0'.", "License")
      val website = askForInput("Please specify a optional website about you or the plugin", "Website")
      val sourceRepo = askForInput("The repository where the code of this plugin is hosted, if published.", "Source repo")
      val bugtracker = askForInput("A optional bug tracker or other support website.", "Bug tracker")

      val metadata = PluginMetadata(description, licence, website, sourceRepo, bugtracker)

      createPlugin(name, author, version, pluginFolderName, api, metadata, language)
    }
  }

  private def createPlugin(name: String, author: String, version: String, pluginFolderName: String, apiVersion: (Int, Int),
                           metadata: PluginMetadata, language: PluginLanguage.Value): Unit = {

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

        if (plugin.createSourceFile(language)) {
          logger info s"Successfully generated the main source file in ${language.toString.toLowerCase}."
        } else {
          logger warn "Unable to generate the main source file."
        }

        if (plugin.createPluginXMLFile(metadata, author, version, apiVersion)) {
          logger info "Successfully created plugin.xml containing metadata."
        } else {
          logger warn "Unable to create plugin.xml containing metadata in plugin resources."
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

  @tailrec
  private def askForInput(information: String, description: String, validate: String => Boolean = _ => true,
                          validationDescription: String = ""): String = {
    println(information)
    print(s"$description > ${if (BuildUtils.isRunningOnWindows) "\n" else ""}")

    val input = scala.io.Source.fromInputStream(System.in).bufferedReader().readLine()
    println("")

    if (validate(input))
      input
    else {
      if (validationDescription.nonEmpty)
        println(s"Your entered value is not valid: $validationDescription")
      askForInput(information, description, validate, validationDescription)
    }
  }

  private def askForInput(information: String, description: String, default: String): String = {
    val input = askForInput(information, description, _ => true)

    if (input.isEmpty) default
    else input
  }

  /**
   * Gets the version of the api, if loaded by sbt.
   *
   * @return a tuple with major and minor version
   */
  def getApiVersion: Def.Initialize[Task[Option[(Int, Int)]]] = Def.task {
    val apiVersion: Option[String] = Def.taskDyn {
      val apiProject = buildStructure.value.allProjectRefs.find(_.project == "apiProject")
      if (apiProject.isDefined)
        Def.task[Option[String]] {
          Some((apiProject.get / version).value)
        }
      else
        Def.task[Option[String]] {
          None // Api hasn't been loaded, probably not fetched
        }
    }.value

    apiVersion.flatMap(ver => Try {
      val parts = ver.split("[.-]") // Also split at '-' to get rid of suffixes like -SNAPSHOT
      (parts.head.toInt, parts(1).toInt)
    }.toOption)
  }
}