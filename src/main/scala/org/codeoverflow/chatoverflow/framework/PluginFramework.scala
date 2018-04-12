package org.codeoverflow.chatoverflow.framework

import java.io.File
import java.net.URLClassLoader

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.api.APIVersion
import org.codeoverflow.chatoverflow.api.plugin.{Pluggable, Plugin, PluginManager}

import scala.collection.mutable

class PluginFramework(val pluginDirectoryPath: String) {
  val pluginDirectory = new File(pluginDirectoryPath)

  private val plugins = mutable.Map[String, Plugin]()

  // TODO: Switch from PluginName to Author.PluginName as identifier for maps
  private val logger = Logger.getLogger(this.getClass)
  private val pluginInfos = mutable.Map[String, PluginInfo]()

  /**
    * Initializes the Plugin Framework by loading all available Plugins from the plugin folder.
    *
    * @param pluginManager the plugin manager to initialize plugins with
    */
  def init(pluginManager: PluginManager): Unit = {
    if (!pluginDirectory.exists() || !pluginDirectory.isDirectory) {
      logger warn s"Plugin directory '$pluginDirectory' does not exist!"
    } else {

      // Get jar file urls
      val jarFiles = pluginDirectory.listFiles().filter(_.getName.toLowerCase.endsWith(".jar"))
      val jarUrls = for (jarFile <- jarFiles) yield jarFile.toURI.toURL

      logger info s"Found ${jarFiles.length} plugins."

      // Create own class loader
      val classLoader = new URLClassLoader(jarUrls)

      logger debug "Start plugin gathering process NOW!"

      // Load pluggables
      val pluggables = loadAllPluggables(jarFiles, classLoader)

      logger debug "Finished plugin gathering process."

      logger info s"Framework API Version is ${APIVersion.MAJOR_VERSION}.${APIVersion.MINOR_VERSION}"

      // Check pluggables, load plugins
      for (pluggable <- pluggables) {

        logger info s"Testing plugin: ${pluggable.getName}"

        // Check plugin version first
        if (pluggable.getMajorAPIVersion != APIVersion.MAJOR_VERSION) {
          logger warn s"API Version (Major) of plugin is ${pluggable.getMajorAPIVersion}. Loading aborted."
          // TODO: Gather not loaded plugins for visualization
        } else {

          if (pluggable.getMinorAPIVersion != APIVersion.MINOR_VERSION) {
            logger debug s"API Version (Minor) of plugin is ${pluggable.getMajorAPIVersion}. Trying to load now."
          } else {
            logger info s"API Version of plugin is valid."
          }

          // Now try to load that plugin
          val plugin = loadPlugin(pluggable, pluginManager)
          if (plugin.isEmpty) {
            logger warn "Plugin loading FAILED!"
            // TODO: Gather not loaded plugins for visualization
          } else {
            logger info s"Loaded plugin ${pluggable.getName} successfull!"

            // Insert the plugin
            plugins += pluggable.getName -> plugin.get
            pluginInfos += pluggable.getName -> pluggable
          }
        }
      }

      // Info
      logger info s"Successfully loaded ${plugins.toList.length} / ${pluggables.length} Plugins (${jarFiles.length} files)!"
    }
  }

  /**
    * Loads all plugins from all found jar files at once.
    */
  private def loadAllPluggables(jarFiles: Seq[File], classLoader: ClassLoader): Seq[Pluggable] =
    (for (jarFile <- jarFiles) yield PluggableLoader.loadPluggables(jarFile, classLoader)).flatten

  /**
    * Trying to load a plugin. Might fail, if the API version is not the Plugin version.
    */
  private def loadPlugin(pluggable: Pluggable, pluginManager: PluginManager): Option[Plugin] = {
    try {
      Some(pluggable.getPlugin(pluginManager))
    } catch {
      case _: Exception => None
    }
  }

  /**
    * Retrieves the plugin information of a given (loaded) plugin name.
    *
    * @param pluginName the name of the plugin
    * @return information about name, author and description
    */
  def getPluginInfo(pluginName: String): Option[PluginInfo] = {
    if (pluginInfos.contains(pluginName))
      Some(pluginInfos(pluginName))
    else
      None
  }

  /**
    * Returns all names of loaded plugins
    *
    * @return a list with all loaded plugin names
    */
  def getLoadedPlugins: List[String] = pluginInfos.keys.toList

  def asyncStartPlugin(pluginName: String): Boolean = ???

  implicit def toPluginInfo(pluggable: Pluggable): PluginInfo =
    PluginInfo(pluggable.getName, pluggable.getAuthor, pluggable.getDescription)

}

object PluginFramework {

  def apply(pluginDirectoryPath: String): PluginFramework = new PluginFramework(pluginDirectoryPath)

}

case class PluginInfo(name: String, author: String, description: String)