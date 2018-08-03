package org.codeoverflow.chatoverflow.configuration

import scala.xml.{Elem, Node}

trait Configuration {
  def toXml: xml.Elem
}

case class PluginInstance(pluginName: String, pluginAuthor: String, instanceName: String,
                          var requirements: Seq[RequirementConfig]) extends Configuration {
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
      <requirements>
        {for (req <- requirements) yield req.toXml}
      </requirements>
    </pluginInstance>

  def this(xmlNode: Node) = this(
    (xmlNode \ "pluginName").text,
    (xmlNode \ "pluginAuthor").text,
    (xmlNode \ "instanceName").text,
    for (node <- xmlNode \ "requirements" \ "_") yield new RequirementConfig(node))

}

case class RequirementConfig(uniqueRequirementId: String, name: String, isOptional: Boolean,
                             targetType: Class[_], serializedContent: String) extends Configuration {

  override def toXml: Elem =
    <requirement>
      <uniqueRequirementId>
        {uniqueRequirementId}
      </uniqueRequirementId>
      <name>
        {name}
      </name>
      <isOptional>
        {isOptional}
      </isOptional>
      <targetType>
        {targetType.getName}
      </targetType>
      <content>
        {serializedContent}
      </content>
    </requirement>

  def this(xmlNode: Node) = this(
    (xmlNode \ "uniqueRequirementId").text,
    (xmlNode \ "name").text,
    if ((xmlNode \ "isOptional").text == "true") true else false,
    Class.forName((xmlNode \ "targetType").text),
    (xmlNode \ "content").text
  )
}

case class ConnectorInstance(connectorType: String, sourceIdentifier: String) extends Configuration {
  override def toXml: Elem =
    <connectorInstance>
      <connectorType>
        {connectorType}
      </connectorType>
      <sourceIdentifier>
        {sourceIdentifier}
      </sourceIdentifier>
    </connectorInstance>

  def this(xmlNode: Node) = this(
    (xmlNode \ "connectorType").text,
    (xmlNode \ "sourceIdentifier").text)
}

// Insert new config options here