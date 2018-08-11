package org.codeoverflow.chatoverflow.configuration

import scala.xml.{Elem, Node}

/**
  * Configurations are DTO-ready objects, which can be easily serialized and deserialized for saving the current configuration.
  * It is recommended to add an additional constructor to work with xml nodes.
  */
trait Configuration {

  /**
    * Returns a xml node with all configuration variables serialized.
    *
    * @return a xml node, ready to save
    */
  def toXml: xml.Elem
}

/**
  * Configuration class for user created plugin instances from previously loaded plugins.
  *
  * @param pluginName   the name of the base plugin to create an instance from
  * @param pluginAuthor the author of this plugin
  * @param instanceName the name of the plugin instance to create
  * @param requirements a seq of requirements, see the RequirementConfig-Object
  */
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

/**
  * A requirement config object encapsulates all needed information to set requirements of a plugin
  *
  * @param uniqueRequirementId the plugin unique requirement id of a single requirement
  * @param targetType          the generic target type of the requirement, matched in loading process
  * @param serializedContent   the serialized content, highly requirement type depended
  */
case class RequirementConfig(uniqueRequirementId: String, targetType: String, serializedContent: String) extends Configuration {

  override def toXml: Elem =
    <requirement>
      <uniqueRequirementId>
        {uniqueRequirementId}
      </uniqueRequirementId>
      <targetType>
        {targetType}
      </targetType>
      <content>
        {serializedContent}
      </content>
    </requirement>

  def this(xmlNode: Node) = this(
    (xmlNode \ "uniqueRequirementId").text,
    (xmlNode \ "targetType").text,
    (xmlNode \ "content").text
  )
}

/**
  * This class saves the state of a loaded connector for sources.
  *
  * @param connectorType    the type of source to instantiate the connector later
  * @param sourceIdentifier e.g. the login name of a input source
  */
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