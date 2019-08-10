package org.codeoverflow.chatoverflow.build.plugins

import scala.xml.Node

/**
 * Equivalent to the one in the main framework, just with a toXML method, rather than a fromXML method and an all string constructor.
 * The website, sourceRepo and bug tracker strings have to be checked with the validateURL method in the companion first,
 * because this class just assumes that those are valid urls.
 * If those aren't the framework will be unable to load these urls, but the actual plugin will work fine.
 *
 * Check the metadata class in the framework for more information about these properties.
 */
case class PluginMetadata(description: String,
                          license: String,
                          website: String,
                          sourceRepo: String,
                          bugtracker: String) {

  /**
   * Converts the metadata into a scala xml object. Empty properties are ignored.
   *
   * @return the metadata as a scala xml node.
   *         It has a root tag with the name plugin and all metadata properties are tags in this root tag.
   */
  def toXML: List[Node] = {
    // Map of tag names to variables. Add new vars here and in the constructor.
    Map(
      "description" -> description,
      "licence" -> license,
      "website" -> website,
      "sourceRepo" -> sourceRepo,
      "bugtracker" -> bugtracker
    ).filter(_._2.nonEmpty) // filters not specified options
      .map(entry => {
        // just dummy tag name, replaced afterwards
        <value>
          {entry._2}
        </value>.copy(label = entry._1) // update the tag name with the correct one
      }).toList
  }
}
