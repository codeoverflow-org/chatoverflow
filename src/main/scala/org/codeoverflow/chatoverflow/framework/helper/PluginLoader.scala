package org.codeoverflow.chatoverflow.framework.helper

import java.io.File

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.plugin.Plugin
import org.codeoverflow.chatoverflow.framework.PluginType
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.ConfigurationBuilder

import scala.collection.JavaConverters._
import scala.xml.{SAXParseException, XML}

/**
  * PluginLoader contains all logic to load plugins from their .jar files.
  * This includes parsing of the 'plugin.xml' file in their resources and
  * loading the main class of the plugin using a classloader.
  *
  * @param jar the jar of the plugin, which this instance is intended to load
  */
class PluginLoader(private val jar: File) extends WithLogger {
  private val jarUrl = jar.toURI.toURL
  // Every plugin needs its own classloader, otherwise 'plugin.xml' will always be the one from the last plugin on the classpath.
  // This also ensures that a plugin can't override some class in another plugin, because of name conflicts.
  private val classloader = new PluginClassLoader(Array(jarUrl))

  /**
    * Loads the plugin from the jar.
    *
    * @return a instance if everything was successfully. None otherwise in which case a error has been logged.
    */
  def loadPlugin(): Option[PluginType] = {
    val cls = findPluginClass()

    if (cls.isEmpty)
      None
    else
      parsePluginXMLFile(cls.get)
  }

  /**
    * Parses the xml file in the plugin jar and creates a instance of PluginType with the provided class.
    *
    * @param cls the class of the class in the plugin, passed to PluginType
    * @return a PluginType, if everything was successfully.
    *         None, if the xml is invalid. In this case a error has been logged to inform the user.
    */
  private def parsePluginXMLFile(cls: Class[_ <: Plugin]): Option[PluginType] = {
    val is = classloader.getResourceAsStream("plugin.xml")
    if (is == null) {
      logger warn s"Can't load plugin from '${jar.getName}': it must contain a 'plugin.xml'."
      return None
    }

    try {
      val xml = XML.load(is)

      val p = xml \\ "plugin"
      if (p.length != 1) {
        logger warn s"Can't load plugin from '${jar.getName}': requires exactly one root 'plugin' tag. Currently having ${p.length}."
        return None
      }

      val api = p \ "api"
      val majorString = (api \ "major").text
      val minorString = (api \ "minor").text

      if (majorString.isEmpty || minorString.isEmpty) {
        logger warn s"Can't load plugin from '${jar.getName}': api versions are missing!"
        return None
      } else if (!isVersionNumber(majorString) || !isVersionNumber(minorString)) {
        logger warn s"Can't load plugin from '${jar.getName}': api versions must be positive numbers!"
        return None
      }

      val pType = new PluginType(
        (p \ "name").text,
        (p \ "author").text,
        (p \ "description").text,
        majorString.toInt,
        minorString.toInt,
        cls
      )

      if (pType.getName.isEmpty || pType.getAuthor.isEmpty || pType.getDescription.isEmpty) {
        logger warn s"Can't load plugin from '${jar.getName}': name, author and/or description missing."
        None
      } else
        Some(pType)
    } catch {
      case e: SAXParseException =>
        logger warn s"Can't load plugin from '${jar.getName}' because the xml file is invalid. Error message: $e"
        None
    }
  }

  /**
    * Util method for parsePluginXMLFile. Checks whether the passed string is a number and is positive.
    *
    * @param s the string to check
    * @return true, if the string is a valid version number
    */
  private def isVersionNumber(s: String): Boolean = s.nonEmpty && s.forall(_.isDigit) && s.toInt >= 0

  /**
    * Finds a class in the jar, that is implementing the plugin interface.
    * If it finds none or more than one it will log a complaint and return None.
    *
    * @return the found class, if exactly one. None otherwise.
    */
  private def findPluginClass(): Option[Class[_ <: Plugin]] = {
    val r = new Reflections(new ConfigurationBuilder()
      .addClassLoader(classloader)
      .setUrls(jarUrl)
      .setScanners(new SubTypesScanner()))

    val classes = r.getSubTypesOf(classOf[Plugin])
      .asScala.filter(_.getClassLoader == classloader) // otherwise includes PluginImpl from main classloader that loads the framework

    classes.size match {
      case 1 => classes.headOption
      case 0 =>
        logger warn s"Can't load plugin from '${jar.getName}': plugin class missing!"
        None
      case count =>
        logger warn s"Can't load plugin from '${jar.getName}': only one plugin per jar file is allowed, not $count!"
        None
    }
  }
}
