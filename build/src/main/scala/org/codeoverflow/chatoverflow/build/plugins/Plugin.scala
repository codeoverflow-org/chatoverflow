package org.codeoverflow.chatoverflow.build.plugins

import java.io.File

import org.codeoverflow.chatoverflow.build.{BuildUtils, SbtFile}
import sbt.io.IO

import scala.annotation.tailrec
import scala.util.Try
import scala.xml.PrettyPrinter

/**
 * A plugin represents a directory in a plugin source directory. Every plugin has its own build file and source folder.
 *
 * @param pluginSourceDirectoryName the plugin base source directory
 * @param name                      the name of the plugin
 */
class Plugin(val pluginSourceDirectoryName: String, val name: String) {
  val normalizedName: String = Plugin.toNormalizedName(name)
  val pluginDirectoryPath: String = s"$pluginSourceDirectoryName/$name"

  /**
   * Creates the plugin folder inside of a plugin source directory
   *
   * @return true, if the process was successful
   */
  def createPluginFolder(): Boolean = {
    val pluginDirectory = new File(pluginDirectoryPath)

    if (pluginDirectory.exists()) {
      false
    } else {
      pluginDirectory.mkdir()
      true
    }
  }

  /**
   * Creates a simple sbt file with name and version info into the plugin folder
   *
   * @param version the version of the sbt plugin project
   * @param language the language the plugin is written in, if it isn't scala, the scala lib will be excluded
   * @return true, if the process was successful
   */
  def createSbtFile(version: String, language: PluginLanguage.Value): Boolean = {
    val sbtFile = new SbtFile(name, version, List(), "", "", false,
      excludeScalaLib = language != PluginLanguage.SCALA)

    // The name of the sbt file is the plugin name. This worked in first tests
    sbtFile.save(s"$pluginDirectoryPath/$normalizedName.sbt")
  }

  /**
   * Generates the plugin.xml file in the resources of the plugin.
   *
   * @param metadata the metadata for this plugin
   * @param author   author of this plugin, used by the framework to identify it
   */
  def createPluginXMLFile(metadata: PluginMetadata, author: String, version: String, apiVersion: (Int, Int)): Boolean = {
    val xml = <plugin>
      <name>
        {name}
      </name>
      <author>
        {author}
      </author>
      <version>
        {version}
      </version>
      <api>
        <major>
          {apiVersion._1}
        </major>
        <minor>
          {apiVersion._2}
        </minor>
      </api>{metadata.toXML}
    </plugin>

    val trimmed = scala.xml.Utility.trim(xml)
    val prettyXml = new PrettyPrinter(100, 2).format(trimmed)

    Try(
      IO.write(new File(s"$pluginDirectoryPath/src/main/resources/plugin.xml"), prettyXml)
    ).isSuccess
  }

  /**
   * Generates the main class file implementing PluginImpl for the plugin developer in their choosen language.
   *
   * @param language the language in with the source file will be generated
   * @return true, if everything was successful
   */
  def createSourceFile(language: PluginLanguage.Value): Boolean = {
    val content = PluginLanguage.getSourceFileContent(normalizedName, language)
    val langName = language.toString.toLowerCase

    Try(
      IO.write(new File(s"$pluginDirectoryPath/src/main/$langName/${normalizedName}Plugin.$langName"), content.getBytes)
    ).isSuccess
  }

  /**
   * Fetches the build plugin jar from the target folder.
   *
   * @return a seq of found jar files or a empty seq (e.g. if the target folder is not found)
   */
  def getBuildPluginFiles: Set[File] = {

    val pluginTargetFolder = new File(s"$pluginDirectoryPath/target/")

    if (!pluginTargetFolder.exists()) {
      Set[File]()
    } else {
      BuildUtils.getAllDirectoryChilds(pluginTargetFolder).filter(_.getName.endsWith(".jar"))
    }
  }

  /**
   * Deletes jars of all versions of this plugin from the plugin target directory.
   *
   * @param pluginTargetFolder the directory which contains all plugins including old versions of this plugin
   */
  def deleteOldJars(pluginTargetFolder: File): Unit = {
    pluginTargetFolder.listFiles()
      .filter(file => file.getName.startsWith(name))
      .foreach(file => file.delete())
  }

  /**
   * Returns whether this is an plugin which is using scala.
   * This is determined by checking if at least one .scala file exists.
   */
  def isScala: Boolean = {
    BuildUtils.getAllDirectoryChilds(new File(s"$pluginDirectoryPath/src/main"))
      .exists(_.getName.endsWith(".scala"))
  }
}

object Plugin {

  /**
   * Checks if the plugin source folder is already used to store build jar plugin files.
   *
   * @param pluginSourceFolderName  the name of the source folder to test
   * @param pluginTargetFolderNames all names of target plugin folders
   * @return true, if the plugin source folder name does NOT exist in the plugin target folder list
   */
  def isSourceFolderNameValid(pluginSourceFolderName: String, pluginTargetFolderNames: List[String]): Boolean =
    !pluginTargetFolderNames.map(_.toLowerCase).contains(pluginSourceFolderName.toLowerCase)

  /**
   * Returns a list of all plugins in a given plugin source folder.
   *
   * @param pluginSourceFolderName the defined source folder of plugins
   * @return a seq of plugins
   */
  def getPlugins(pluginSourceFolderName: String): Seq[Plugin] = {
    val pluginSourceFolder = new File(pluginSourceFolderName)
    pluginSourceFolder.listFiles
      .filter(_.isDirectory)
      .filter(d => containsPluginXMLFile(d))
      .map(folder => new Plugin(pluginSourceFolderName, folder.getName))

  }

  /**
   * Checks, if a plugin source folder exsists
   *
   * @param pluginSourceFolderName the source folder name
   * @return true, if exists and is directory
   */
  def sourceFolderExists(pluginSourceFolderName: String): Boolean = {
    val pluginSourceFolder = new File(pluginSourceFolderName)
    pluginSourceFolder.exists() && pluginSourceFolder.isDirectory
  }

  @tailrec
  private def toNormalizedName(name: String): String = {
    if (name.isEmpty) {
      return ""
    }

    val firstChar = name(0)

    if (!Character.isJavaIdentifierStart(firstChar)) {
      toNormalizedName(name.substring(1))
    } else {
      val rest = name.substring(1)
      firstChar + rest.filter(Character.isJavaIdentifierPart)
    }
  }

  private def containsPluginXMLFile(directory: File): Boolean = {
    new File(s"$directory/src/main/resources/plugin.xml").exists()
  }
}


