package org.codeoverflow.chatoverflow.config

import scala.xml.{Elem, Node}

trait ConfigEntry {
  def toXml: xml.Elem
}

case class PluginInstanceConfigEntry(pluginName: String, pluginAuthor: String, instanceName: String) extends ConfigEntry {
  override def toXml: Elem =
    <pluginInstance>
      <pluginName>
        {pluginName}
      </pluginName>
      <pluginAuthor>
        {pluginAuthor}
      </pluginAuthor>
      <instanceName>
        {instanceName}
      </instanceName>
    </pluginInstance>

  def this(xmlNode: Node) = this(
    (xmlNode \ "pluginName").text,
    (xmlNode \ "pluginAuthor").text,
    (xmlNode \ "instanceName").text)

}