package org.codeoverflow.chatoverflow.framework

import java.io.File

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.framework.helper.PluginLoader
import org.codeoverflow.chatoverflow.framework.manager.PluginManagerStub

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Success, Try}

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
  def getPlugins: List[PluginType] = {
    // We are reevaluating this each call because there may be new plugins that needed longer for e.g. dependency resolution
    // or that were loaded after initial startup.
    pluginTypes
      .groupBy(pt => (pt.getAuthor, pt.getName)).values // Create a list for each plugin with its different versions
      .map(versions => versions.maxBy(pt => pt.getVersion)).toList // Selects the newest (alphabetically last) version
  }

  /**
    * Checks the plugin directory path for not yet loaded jar files, searches for pluggable definitions
    * and adds compatible plugins to the plugin type list.
    */
  def loadNewPlugins(): Unit = {
    val pluginDirectory = new File(pluginDirectoryPath)

    if (!pluginDirectory.exists() || !pluginDirectory.isDirectory) {
      logger warn s"PluginType directory '$pluginDirectory' does not exist!"
    } else {

      val futures = ListBuffer[Future[_]]()

      // Get (new) jar file urls
      val jarFiles = getNewJarFiles(pluginDirectory)
      logger info s"Found ${jarFiles.length} new plugins."

      for (jarFile <- jarFiles) {

        // Load plugin from jar
        val pluginOpt = new PluginLoader(jarFile).loadPlugin()

        // if something went wrong just carry on to the next jar.
        // Warnings about loading the plugin will be printed by PluginLoader.
        if (pluginOpt.isDefined) {
          val plugin = pluginOpt.get

          if (plugin.testState == PluginCompatibilityState.NotCompatible) {
            logger warn s"Unable to load plugin '$plugin' due to different major versions."
          } else {

            // Try to test the initiation of the plugin
            futures += plugin.getDependencyFuture andThen {
              case Success(_) =>
                try {
                  if (plugin.createPluginInstance(new PluginManagerStub).isDefined) {
                    logger info s"Successfully tested instantiation of plugin '$plugin'."
                    pluginTypes += plugin
                  } else {
                    logger info s"Instantiation test of plugin '$plugin' failed."
                  }
                } catch {
                  // Note that we catch not only exceptions, but also errors like NoSuchMethodError. Deep stuff
                  case _: Error => logger warn s"Error while test init of plugin '$plugin'."
                  case _: Exception => logger warn s"Exception while test init of plugin '$plugin'."
                }
            }
          }
        }
      }

      // If plugins aren't done within this timeout they can still fetch everything in the background, they just won't be included in this summary
      Try(futures.foreach(f => Await.ready(f, 5.seconds))) // Await.ready throws an exception if the timeout is hit, ignore that!

      logger info s"Loaded ${pluginTypes.length} plugin types in total: " +
        s"${pluginTypes.mkString(", ")}"

      // Inform the user of any plugins with multiple installed versions
      // The newest version is automatically selected in the "getPlugins" method when it is used.
      pluginTypes.groupBy(pt => (pt.getAuthor, pt.getName)).values // Create a list for each plugin with its different versions
        .filter(_.size > 1) // Only show warning when there are multiple versions
        .foreach { pts =>
          val newest = pts.maxBy(_.getVersion)
          logger warn s"There are ${pts.size} different version of the '${newest.getName} (${newest.getAuthor})' plugin installed: " +
            s"${pts.map(_.getVersion).sorted.mkString(", ")}. Using the newest version: $newest."
        }
    }
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