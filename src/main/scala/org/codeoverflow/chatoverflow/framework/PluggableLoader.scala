package org.codeoverflow.chatoverflow.framework

import java.io.{File, FileInputStream}
import java.util.jar.{JarEntry, JarInputStream}

import org.codeoverflow.chatoverflow.api.plugin.Pluggable

import scala.collection.mutable.ListBuffer

object PluggableLoader {

  def loadPluggables(jar: File, cl: ClassLoader): Seq[Pluggable] = {

    // First: Extract all pluggable classes from the jar file
    val pluggableClasses = extractPluggablesFromJarFile(jar, cl)

    // Now: Try to instantiate the pluggable class
    val pluggableObjects = createPluggablObjects(pluggableClasses)

    // TODO: Import logger and warn, if there is more than one! (Maybe possible, but not defined)
    // TODO: Get rid of the printlns, where are we?

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
            System.err.println(s"Can't load Class ${entry.getName}")
            e.printStackTrace()
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
          System.err.println(s"Can't instantiate plugin: ${plug.getName}")
          e.printStackTrace();
        case e: IllegalAccessException =>
          System.err.println(s"IllegalAccess for plugin: ${plug.getName}")
          e.printStackTrace();
      }
    }

    pluggableObjects.toList
  }

}
