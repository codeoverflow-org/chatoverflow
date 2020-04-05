package org.codeoverflow.chatoverflow.framework.helper

import java.io.File

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.plugin.Plugin
import org.codeoverflow.chatoverflow.framework.{PluginMetadata, PluginType}
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.ConfigurationBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success}
import scala.xml.{Node, SAXParseException, XML}

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

      val pluginElems = xml \\ "plugin"
      if (pluginElems.length != 1) {
        logger warn s"Can't load plugin from '${jar.getName}': requires exactly one 'plugin' tag." +
          s" Currently having ${pluginElems.length}."
        return None
      }

      val p = pluginElems.head
      val api = (p \ "api").head
      val majorString = getString(api, "major")
      val minorString = getString(api, "minor")

      if (!isVersionNumber(majorString) || !isVersionNumber(minorString)) {
        logger warn s"Can't load plugin from '${jar.getName}': api versions must be positive numbers!"
        return None
      }

      Some(new PluginType(
        getString(p, "name"),
        getString(p, "author"),
        getString(p, "version"),
        majorString.toInt,
        minorString.toInt,
        PluginMetadata.fromXML(p),
        cls,
        resolveDependencies(getString(p, "name"))
      ))
    } catch {
      // thrown by getString
      case e: IllegalArgumentException =>
        logger warn s"Can't load plugin from '${jar.getName}': ${e.getMessage}"
        None
      case e: SAXParseException =>
        logger warn s"Can't load plugin from '${jar.getName}' because the xml file is invalid. Error message: $e"
        None
    }
  }

  /**
    * Helper method for parsePluginXMLFile, which gets the value of a tag if it occurs exactly once and isn't empty.
    * Throws a IllegalArgumentException if the tag is missing, occurs multiple times or is empty.
    * The advantage of a exception rather than option is that we don't need to explicitly check
    * every single call to this method. parsePluginXMLFile can just print a warning when this exception is thrown.
    *
    * @return the value of the requested tag
    */
  private def getString(n: Node, tag: String): String = {
    val elems = n \ tag
    elems.size match {
      case 1 if elems.text.nonEmpty => elems.text
      case 1 => throw new IllegalArgumentException(s"'$tag' in the plugin.xml is empty")
      case 0 => throw new IllegalArgumentException(s"'$tag' in the plugin.xml is missing")
      case _ => throw new IllegalArgumentException(s"only one tag with the name '$tag' is allowed in the plugin.xml")
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

  /**
   * Creates a future which gets all dependencies from the included dependencies.pom, if existing, fetches them
   * and adds their jar files to the classloader.
   *
   * @param pluginName the name of the plugin, only used for logging
   * @return a future of all required jars for this plugin
   */
  private def resolveDependencies(pluginName: String): Future[Seq[File]] = {
    val pomIs = classloader.getResourceAsStream("dependencies.pom")
    if (pomIs == null) {
      return Future(Seq())
    }

    Future(CoursierUtils.parsePom(pomIs))
      .map(dependencies => dependencies.filter(!_.module.name.value.startsWith("chatoverflow-api")))
      .map(dependencies => CoursierUtils.fetchDependencies(dependencies))
      .andThen {
        case Success(jarFiles) =>
          jarFiles.foreach(jar => classloader.addURL(jar.toURI.toURL))
          logger info s"Dependencies for the plugin $pluginName successfully resolved and fetched if missing."
        case Failure(exception) =>
          logger warn s"Couldn't resolve and fetch dependencies for the plugin in $pluginName: $exception"
      }
  }
}
