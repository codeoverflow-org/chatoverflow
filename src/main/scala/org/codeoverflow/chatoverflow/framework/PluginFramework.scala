package org.codeoverflow.chatoverflow.framework

import java.io.File

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.framework.helper.{PluggableLoader, PluginClassLoader}
import org.codeoverflow.chatoverflow.framework.manager.PluginManagerStub

import scala.collection.mutable.ListBuffer

/**
  * The plugin framework holds all plugin types important from the jar files in the plugin folder.
  *
  * @param pluginDirectoryPath the path to the plugin folder
  */
class PluginFramework(pluginDirectoryPath: String) extends WithLogger {
  private val pluginTypes: ListBuffer[PluginType] = ListBuffer[PluginType]()
  private val loadedJarPaths: ListBuffer[String] = ListBuffer[String]()

  /**
    * Returns, if a plugin type exists.
    *
    * @param pluginName   the name of the plugin set in the plugin jar file
    * @param pluginAuthor the author of the plugin
    * @return true, if the specified plugin type exists
    */
  def pluginExists(pluginName: String, pluginAuthor: String): Boolean = {
    getPlugin(pluginName, pluginAuthor).isDefined
  }

  /**
    * Returns the plugin type specified by name and author.
    *
    * @param pluginName   the name of the plugin set in the plugin jar file
    * @param pluginAuthor the author of the plugin
    * @return a plugin type retrieved from the plugin framework registry
    */
  def getPlugin(pluginName: String, pluginAuthor: String): Option[PluginType] = {
    val candidates = getPlugins.filter(p => p.getName.equals(pluginName) && p.getAuthor.equals(pluginAuthor))
    if (candidates.length != 1) {
      None
    } else {
      Some(candidates.head)
    }
  }

  /**
    * Returns a list of all currently loaded plugin types who might be API version compatible
    *
    * @return a list of PluginType definitions
    */
  def getPlugins: List[PluginType] = pluginTypes.toList

  /**
    * Checks the plugin directory path for not yet loaded jar files, searches for pluggable definitions
    * and adds compatible plugins to the plugin type list.
    */
  def loadNewPlugins(): Unit = {
    val pluginDirectory = new File(pluginDirectoryPath)

    if (!pluginDirectory.exists() || !pluginDirectory.isDirectory) {
      logger warn s"PluginType directory '$pluginDirectory' does not exist!"
    } else {

      // Get (new) jar file urls
      val jarFiles = getNewJarFiles(pluginDirectory)
      val jarUrls = for (jarFile <- jarFiles) yield jarFile.toURI.toURL
      logger info s"Found ${jarFiles.length} new plugins."

      // Create own class loader and PluggableLoader
      val classLoader = new PluginClassLoader(jarUrls)
      val pluggableLoader = new PluggableLoader(classLoader)

      // Load pluggables
      for (jarFile <- jarFiles) {

        // Get pluggables
        val pluggables = pluggableLoader.loadPluggables(jarFile)

        // Create plugin types
        for (pluggable <- pluggables) {
          val plugin = new PluginType(pluggable)

          if (plugin.testState == PluginCompatibilityState.NotCompatible) {
            logger warn s"Unable to load plugin '${plugin.getName}' due to different major versions."
          } else {

            // Try to test the initiation of the plugin
            try {
              plugin.createPluginInstance(new PluginManagerStub)
              logger info s"Successfully tested instantiation of plugin '${plugin.getName}'"
              pluginTypes += plugin
            } catch {
              case _: Exception => logger warn s"Error while test init of plugin '${plugin.getName}'."
            }
          }
        }
      }
    }

    logger info s"Loaded ${pluginTypes.length} plugin types in total: " +
      s"${pluginTypes.map(pt => s"${pt.getName} (${pt.getAuthor})").mkString(", ")}"
  }

  /**
    * Returns all jar files that are not loaded yet.
    */
  private def getNewJarFiles(pluginDirectory: File): Array[File] = {
    val allJarFiles = pluginDirectory.listFiles().filter(_.getName.toLowerCase.endsWith(".jar"))
    val newJarFiles = allJarFiles.filter(f => !loadedJarPaths.contains(f.getPath))
    loadedJarPaths ++= newJarFiles.map(_.getPath)

    newJarFiles
  }
}