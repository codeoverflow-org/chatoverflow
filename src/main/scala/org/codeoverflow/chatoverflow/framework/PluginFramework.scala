package org.codeoverflow.chatoverflow.framework

import java.io.File

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.api.APIVersion
import org.codeoverflow.chatoverflow.api.plugin.{Pluggable, Plugin, PluginManager}

import scala.collection.mutable

class PluginFramework(val pluginDirectoryPath: String) {

  private val pluginDirectory = new File(pluginDirectoryPath)
  private val fancySeparator = "----------------------------------------------------"
  private val logger = Logger.getLogger(this.getClass)
  logger info s"Started plugin framework with plugin directory '$pluginDirectoryPath'."

  private val pluginInfos = mutable.Map[(String, String), PluginInfo]()
  private val notLoadedPluginInfos = mutable.Map[(String, String), PluginInfo]()
  private val plugins = mutable.Map[(String, String), Plugin]()

  /**
    * Initializes the Plugin Framework by loading all available Plugins from the plugin folder.
    *
    * @param pluginManager the plugin manager to initialize plugins with
    */
  def init(pluginManager: PluginManager): Unit = {

    // Reset state
    pluginInfos.clear()
    plugins.clear()
    notLoadedPluginInfos.clear()

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
      val pluggables = loadAllPluggables(jarFiles, classLoader)

      logger info fancySeparator
      logger debug "Finished plugin gathering process."

      val apiMajorVersion = APIVersion.MAJOR_VERSION
      val apiMinorVersion = APIVersion.MINOR_VERSION
      logger info s"Framework API Version is $apiMajorVersion.$apiMinorVersion"


      // Check pluggables, load plugins
      for (pluggable <- pluggables) {

        logger info s"Plugin: ${pluggable.getName}"
        val pluginMajorVersion = pluggable.getMajorAPIVersion
        val pluginMinorVersion = pluggable.getMinorAPIVersion

        // Check plugin version first
        if (pluginMajorVersion != apiMajorVersion) {
          logger warn s"API Version of plugin is $pluginMajorVersion.$pluginMinorVersion (API: $apiMajorVersion.$apiMinorVersion). Loading aborted."
          notLoadedPluginInfos += (pluggable.getAuthor, pluggable.getName) -> pluggable
        } else {

          if (pluginMinorVersion != apiMinorVersion) {
            logger debug s"API Version of plugin is $pluginMajorVersion.$pluginMinorVersion (API: $apiMajorVersion.$apiMinorVersion). Trying to load now."
          } else {
            logger info s"API Version of plugin is valid ($pluginMajorVersion.$pluginMinorVersion)."
          }

          // Now try to load that plugin
          val plugin = loadPlugin(pluggable, pluginManager)
          if (plugin.isEmpty) {
            logger warn "Plugin loading FAILED!"
            notLoadedPluginInfos += (pluggable.getAuthor, pluggable.getName) -> pluggable
          } else {
            logger info s"Loaded plugin ${pluggable.getName} successfull!"

            // Insert the plugin
            if (!plugins.contains((pluggable.getAuthor, pluggable.getName))) {
              plugins += (pluggable.getAuthor, pluggable.getName) -> plugin.get
              pluginInfos += (pluggable.getAuthor, pluggable.getName) -> pluggable
            } else {
              logger warn s"Unable to manage plugin: '${pluggable.getAuthor}.${pluggable.getName}'. Signature already used."
              notLoadedPluginInfos += (pluggable.getAuthor, pluggable.getName) -> pluggable
            }
          }
        }
      }

      // Info
      logger info fancySeparator
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
      Some(pluggable.createNewPluginInstance(pluginManager))
    } catch {
      case _: Exception => None
    }
  }

  /**
    * Retrieves the plugin information of a given (loaded) plugin name.
    *
    * @param plugin the plugin signature (authorName.pluginName)
    * @return information about name, author and description
    */
  def getPluginInfo(plugin: (String, String)): Option[PluginInfo] = {
    if (pluginInfos.contains(plugin))
      Some(pluginInfos(plugin))
    else if (notLoadedPluginInfos.contains(plugin))
      Some(notLoadedPluginInfos(plugin))
    else
      None
  }

  /**
    * Returns all names of loaded plugins
    *
    * @return a list with all loaded plugin (authorName.pluginName)
    */
  def getLoadedPlugins: List[(String, String)] = pluginInfos.keys.toList

  /**
    * Returns a list of all plugins with loading failures.
    *
    * @return a list of plugin signatures (authorName.pluginName)
    */
  def getNotLoadedPlugins: List[(String, String)] = notLoadedPluginInfos.keys.toList

  /**
    * Creates a new thread and tries to start the plugin
    *
    * @param plugin the plugin signature (authorName.pluginName)
    */
  def asyncStartPlugin(plugin: (String, String)): Unit = {

    // Always escape!
    if (!plugins.contains(plugin)) {
      logger warn s"Plugin '$plugin' was not loaded. Unable to start."
    } else {
      logger info s"Starting plugin '$plugin' in new thread!"
      val loadedPlugin = plugins(plugin)

      try {
        // TODO: Manage all threads, passing arguments, maybe using actors rather than threads
        new Thread(() => {
          try {
            loadedPlugin.start()
          } catch {
            case e: AbstractMethodError => logger.error(s"Plugin '$plugin' just crashed. Looks like a plugin version error.", e)
            case e: Exception => logger.error(s"Plugin '$plugin' just had an exception. Might be a plugin implementation fault.", e)
            case e: Throwable => logger.error(s"Plugin '$plugin' just crashed.", e)
          }
        }).start()
      } catch {
        case e: Throwable => logger.error(s"Plugin starting process (Plugin: '$plugin') just crashed.", e)
      }
    }
  }

  private implicit def toPluginInfo(pluggable: Pluggable): PluginInfo =
    PluginInfo(pluggable.getName, pluggable.getAuthor, pluggable.getDescription)

}

object PluginFramework {

  def apply(pluginDirectoryPath: String): PluginFramework = new PluginFramework(pluginDirectoryPath)

}

case class PluginInfo(name: String, author: String, description: String)