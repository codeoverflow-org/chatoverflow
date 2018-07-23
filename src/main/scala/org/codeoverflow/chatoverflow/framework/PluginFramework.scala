package org.codeoverflow.chatoverflow.framework

import java.io.File

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.api.APIVersion
import org.codeoverflow.chatoverflow.api.plugin.{Pluggable, Plugin, PluginManager}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class PluginFramework(val pluginDirectoryPath: String) {

  private val pluginDirectory = new File(pluginDirectoryPath)
  private val fancySeparator = "----------------------------------------------------"
  private val logger = Logger.getLogger(this.getClass)
  logger info s"Started plugin framework with plugin directory '$pluginDirectoryPath'."

  private val loadedPlugins = ListBuffer[PluginInfo]()
  private val notLoadedPlugins = ListBuffer[PluginInfo]()
  private val pluggables = mutable.Map[PluginId, Pluggable]()

  /**
    * Initializes the Plugin Framework by loading all available Plugins from the plugin folder.
    *
    * @param pluginManager the plugin manager to initialize plugins with
    */
  def init(pluginManager: PluginManager): Unit = {

    // Reset state
    loadedPlugins.clear()
    notLoadedPlugins.clear()
    pluggables.clear()

    if (!pluginDirectory.exists() || !pluginDirectory.isDirectory) {
      logger warn s"Plugin directory '$pluginDirectory' does not exist!"
    } else {

      // Get jar file urls
      val jarFiles = pluginDirectory.listFiles().filter(_.getName.toLowerCase.endsWith(".jar"))
      val jarUrls = for (jarFile <- jarFiles) yield jarFile.toURI.toURL

      logger debug "Start plugin gathering process."
      logger info fancySeparator
      logger info s"Found ${jarFiles.length} plugins."

      // Create own class loader
      val classLoader = new PluginClassLoader(jarUrls)

      // Load pluggables
      val notTestedPluggables = loadAllPluggables(jarFiles, classLoader)

      logger info fancySeparator
      logger debug "Finished plugin gathering process."

      val apiMajorVersion = APIVersion.MAJOR_VERSION
      val apiMinorVersion = APIVersion.MINOR_VERSION
      logger info s"Framework API Version is $apiMajorVersion.$apiMinorVersion"


      // Check pluggables, load plugins
      for (pluggable <- notTestedPluggables) {

        logger info s"Plugin: ${pluggable.getName}"
        val pluginMajorVersion = pluggable.getMajorAPIVersion
        val pluginMinorVersion = pluggable.getMinorAPIVersion

        // Check plugin version first
        if (pluginMajorVersion != apiMajorVersion) {
          logger warn s"API Version of plugin is $pluginMajorVersion.$pluginMinorVersion (API: $apiMajorVersion.$apiMinorVersion). Loading aborted."
          notLoadedPlugins += pluggable
        } else {

          if (pluginMinorVersion != apiMinorVersion) {
            logger debug s"API Version of plugin is $pluginMajorVersion.$pluginMinorVersion (API: $apiMajorVersion.$apiMinorVersion). Trying to load now."
          } else {
            logger info s"API Version of plugin is valid ($pluginMajorVersion.$pluginMinorVersion)."
          }

          // Now try to load that plugin, just to make sure it's possible
          val plugin = loadPlugin(pluggable, pluginManager)
          if (plugin.isEmpty) {
            logger warn "Plugin loading FAILED!"
            notLoadedPlugins += pluggable
          } else {
            logger info s"Loaded plugin ${pluggable.getName} successfull!"

            // Insert the plugin
            if (!pluggables.contains(pluggable)) {
              pluggables += toPluginId(pluggable) -> pluggable
              loadedPlugins += pluggable
            } else {
              logger warn s"Unable to manage plugin: '${pluggable.getAuthor}.${pluggable.getName}'. Signature already used."
              notLoadedPlugins += pluggable
            }
          }
        }
      }

      // Info
      logger info fancySeparator
      logger info s"Successfully loaded ${pluggables.toList.length} / ${notTestedPluggables.length} Plugins (${jarFiles.length} files)!"
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
      Some(pluggable.createNewPluginInstance(pluginManager))
    } catch {
      case _: Exception => None
    }
  }

  /**
    * Returns a pluggable object from the given info.
    *
    * @param pluginInfo the plugin info object. Use getLoadedPlugins() to retrieve these.
    * @return a (already tested) pluggable object ready to instantiate
    */
  def getPluggable(pluginInfo: PluginInfo): Option[Pluggable] = getPluggable(pluginInfoToPluginId(pluginInfo))

  /**
    * Returns a pluggable object from the given info.
    *
    * @param pluginName   name of the plugin
    * @param pluginAuthor name of the plugin author
    * @return a (already tested) pluggable object ready to instantiate
    */
  def getPluggable(pluginName: String, pluginAuthor: String): Option[Pluggable] =
    getPluggable(PluginId(pluginName, pluginAuthor))

  /**
    * Returns a pluggable object from the given info.
    *
    * @param pluginId the plugin id object. Contains plugin name and author.
    * @return a (already tested) pluggable object ready to instantiate
    */
  def getPluggable(pluginId: PluginId): Option[Pluggable] = {
    if (pluggables.contains(pluginId))
      Some(pluggables(pluginId))
    else
      None
  }

  /**
    * Returns all names of loaded plugins
    *
    * @return a list with all loaded plugin infos
    */
  def getLoadedPlugins: List[PluginInfo] = loadedPlugins.toList

  /**
    * Returns a list of all plugins with loading failures.
    *
    * @return a list with all not loaded plugin infos
    */
  def getNotLoadedPlugins: List[PluginInfo] = notLoadedPlugins.toList

  // Implicit conversion for plugin info/id objects
  private implicit def toPluginInfo(pluggable: Pluggable): PluginInfo =
    PluginInfo(pluggable.getName, pluggable.getAuthor, pluggable.getDescription)

  private implicit def toPluginId(pluggable: Pluggable): PluginId =
    PluginId(pluggable.getName, pluggable.getAuthor)

  private implicit def pluginInfoToPluginId(pluginInfo: PluginInfo): PluginId =
    PluginId(pluginInfo.name, pluginInfo.author)

}

object PluginFramework {

  def apply(pluginDirectoryPath: String): PluginFramework = new PluginFramework(pluginDirectoryPath)

}

case class PluginInfo(name: String, author: String, description: String)

case class PluginId(name: String, author: String)