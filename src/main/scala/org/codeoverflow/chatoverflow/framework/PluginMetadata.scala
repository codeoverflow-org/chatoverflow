package org.codeoverflow.chatoverflow.framework

import java.net.{MalformedURLException, URL}

import scala.xml.{Node, NodeSeq}

/* To add more metadata properties just add them to this class and the fromXML method in the companion object.
 * To make them accessible to the rest interface you also need to add them in ui.web.rest.DTOs.PluginMetadata
 * and in the getPluginMetadata method of the TypeController */

/**
  * Contains optional metadata of plugins that the plugin developer may share to the users.
  *
  * @param description a short description of what this plugin does
  * @param licence     the license of the plugin, if it has one. Should be a SPDX short identifier e.g. "MIT".
  *                    Check this url for more information: https://spdx.org/licenses/
  * @param website     a website of the author or plugin
  * @param sourceRepo  the repository on e.g. GitHub, if published.
  * @param bugtracker  a link to a bug tracker where users can report bugs
  */
case class PluginMetadata(description: Option[String],
                          licence: Option[String],
                          website: Option[URL],
                          sourceRepo: Option[URL],
                          bugtracker: Option[URL])

object PluginMetadata {

  /**
    * Parses all metadata from a xml file, that is included in the jar file of all plugins.
    *
    * @param elem the main node of the 'plugin' tag in the xml file
    * @return a PluginMetadata instance with the values of the xml file
    */
  def fromXML(elem: Node): PluginMetadata = PluginMetadata(
    description = getStringOpt(elem \ "description"),
    licence = getStringOpt(elem \ "license"),
    website = getURLOpt(elem \ "website"),
    sourceRepo = getURLOpt(elem \ "sourceRepo"),
    bugtracker = getURLOpt(elem \ "bugtracker"),
  )

  /**
    * Gets the text of a html tag.
    * @return the value of the tag if it exactly occurs only once. None otherwise.
    */
  private def getStringOpt(nodes: NodeSeq): Option[String] = nodes.size match {
    case 1 => Some(nodes.text)
    case _ => None
  }

  /**
    * Like getStringOpt but also parses the value to a url using stringToURL
    */
  private def getURLOpt(nodes: NodeSeq): Option[URL] = {
    val s = getStringOpt(nodes)

    if (s.isEmpty) None
    else stringToURL(s.get)
  }

  /**
    * Parses the passed string into an URL.
    * If the passed string doesn't have a protocol https will be used.
    *
    * @return the url if it could be parsed. None otherwise.
    */
  private def stringToURL(s: String): Option[URL] = {
    try {
      Some(new URL(s))
    } catch {
      case e: MalformedURLException if e.getMessage.contains("no protocol") => stringToURL(s"https://$s")
      case _: Throwable => None
    }
  }
}
