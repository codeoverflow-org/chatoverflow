package org.codeoverflow.chatoverflow.framework

import java.io.{File, FileInputStream}
import java.util.jar.{JarEntry, JarInputStream}

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.api.plugin.Pluggable

import scala.collection.mutable.ListBuffer

object PluggableLoader {

  private val logger = Logger.getLogger(this.getClass)

  /**
    * Loads all pluggables from the specified jar file with the specified class loader.
    *
    * @param jar the jar file to loak into
    * @param cl  the class loader to open the jar file
    * @return a seq of pluggables, found in the jar file
    */
  def loadPluggables(jar: File, cl: ClassLoader): Seq[Pluggable] = {

    logger info s"Trying to load pluggables from jar file: ${jar.getPath}"

    // First: Extract all pluggable classes from the jar file
    val pluggableClasses = extractPluggablesFromJarFile(jar, cl)

    // Now: Try to instantiate the pluggable class
    val pluggableObjects = createPluggablObjects(pluggableClasses)

    // TODO: More than one pluggable class / plugin is not supported right now. But might work?
    pluggableObjects.length match {
      case 0 => logger info "No pluggable classes found."
      case 1 => logger info "Pluggable successful instantiated."
      case _ => logger warn "More than one pluggable class found."
    }

    pluggableObjects
  }

  /**
    * Opens jar file and extracts all classes that implement the Pluggable interface
    */
  private def extractPluggablesFromJarFile(jar: File, cl: ClassLoader): Seq[Class[Pluggable]] = {

    // Open jar file
    val jarInputStream = new JarInputStream(new FileInputStream(jar))
    val classes = ListBuffer[Class[Pluggable]]()

    // Go trough all classes in the jar, search for Pluggable Classes
    var entry: JarEntry = jarInputStream.getNextJarEntry
    while (entry != null) {
      if (entry.getName.toLowerCase.endsWith(".class")) {
        try {
          val name = entry.getName.substring(0, entry.getName.length() - 6).replace('/', '.')
          val cls = cl.loadClass(name)

          // This is the important part
          if (isPluggableClass(cls)) {
            classes += cls.asInstanceOf[Class[Pluggable]]
          }

        }
        catch {
          case e: ClassNotFoundException =>
            logger error(s"Can't load Class ${entry.getName}", e)
        }
      }
      entry = jarInputStream.getNextJarEntry
    }

    // Close stream and return all pluggable classes
    jarInputStream.close()
    classes.toList

  }

  /**
    * A class file must contain a instance of Pluggable to be a pluggable class.
    */
  private def isPluggableClass(clazz: Class[_]): Boolean =
    clazz.getInterfaces.exists(cls => cls equals classOf[Pluggable])

  /**
    * If a class contains a pluggable instance, it can be created
    */
  private def createPluggablObjects(pluggables: Seq[Class[Pluggable]]): List[Pluggable] = {

    val pluggableObjects = new ListBuffer[Pluggable]

    for (plug <- pluggables) {
      try {

        // Try to instantiate the plugin class
        pluggableObjects += plug.newInstance()

      } catch {
        case e: InstantiationException =>
          logger error(s"Can't instantiate plugin: ${plug.getName}", e)
        case e: IllegalAccessException =>
          logger error(s"IllegalAccess for plugin: ${plug.getName}", e)
      }
    }

    pluggableObjects.toList
  }

}