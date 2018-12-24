package org.codeoverflow.chatoverflow2.framework.helper

import java.io.{File, FileInputStream}
import java.util.jar.{JarEntry, JarInputStream}

import org.codeoverflow.chatoverflow.api.plugin.Pluggable
import org.codeoverflow.chatoverflow2.WithLogger

import scala.collection.mutable.ListBuffer

class PluggableLoader(classLoader: ClassLoader) extends WithLogger {

  /**
    * Loads all pluggables from the specified jar file.
    *
    * @param jar the jar file to look into
    * @return a seq of pluggables found in the jar file
    */
  def loadPluggables(jar: File): Seq[Pluggable] = {
    logger info s"Trying to load pluggables from jar file: '${jar.getName}'"

    // First: Find all pluggable classes from the jar file
    val pluggableClasses = findPluggables(jar)

    // Now: Try to instantiate the pluggable class
    val pluggableObjects = createPluggableObjects(pluggableClasses)

    // TODO: Are more than 1 implementation of pluggable allowed / possible / useful
    pluggableObjects.length match {
      case 0 => logger warn "No pluggable classes found."
      case 1 =>
      case _ => logger warn "More than one pluggable class found."
    }

    pluggableObjects
  }

  /**
    * Opens jar file and extracts all classes that implement the Pluggable interface
    */
  private def findPluggables(jar: File): Seq[Class[Pluggable]] = {

    // Open jar file
    val jarInputStream = new JarInputStream(new FileInputStream(jar))
    var classes = ListBuffer[Class[Pluggable]]()

    // Go trough all classes in the jar, search for Pluggable Classes
    var entry: JarEntry = jarInputStream.getNextJarEntry

    while (entry != null) {
      if (entry.getName.toLowerCase.endsWith(".class")) {
        try {
          val name = entry.getName.substring(0, entry.getName.length() - 6).replace('/', '.')
          val clazz = classLoader.loadClass(name)

          // Test for pluggable
          if (implementsPluggable(clazz)) {
            logger info s"Found pluggable '${entry.getName}'."
            classes += clazz.asInstanceOf[Class[Pluggable]]
          }

        }
        catch {
          case e: ClassNotFoundException =>
            logger error(s"Can't load Class '${entry.getName}' from jar file '${jar.getName}.'", e)
        }
      }
      entry = jarInputStream.getNextJarEntry
    }

    // Close stream and return all pluggable classes
    jarInputStream.close()
    classes.toList

  }

  /**
    * Checks, if a class implements the pluggable interface.
    */
  private def implementsPluggable(clazz: Class[_]): Boolean = {
    clazz.getInterfaces.exists(cls => cls equals classOf[Pluggable])
  }

  /**
    * If a class contains a pluggable instance, it can be created
    */
  private def createPluggableObjects(pluggables: Seq[Class[Pluggable]]): List[Pluggable] = {

    val pluggableObjects = ListBuffer[Pluggable]()

    for (plug <- pluggables) {
      try {

        // Try to instantiate the plugin class
        pluggableObjects += plug.newInstance()
        logger info s"Successfully read pluggable '${plug.getName}'."

      } catch {
        case e: InstantiationException =>
          logger error(s"Can't instantiate plugin: '${plug.getName}'.", e)
        case e: IllegalAccessException =>
          logger error(s"IllegalAccess for plugin: '${plug.getName}'.", e)
      }
    }

    pluggableObjects.toList
  }

}