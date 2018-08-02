package org.codeoverflow.chatoverflow.configuration

import scala.xml.{Elem, Node}

trait Configuration {
  def toXml: xml.Elem
}

case class PluginInstance(pluginName: String, pluginAuthor: String, instanceName: String,
                          var sourceRequirements: Seq[SourceRequirementConfig],
                          var parameterRequirements: Seq[ParameterRequirementConfig]) extends Configuration {
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
      <config>
        <sourceRequirements>
          {for (sourceReq <- sourceRequirements) yield sourceReq.toXml}
        </sourceRequirements>
        <parameterRequirements>
          {for (parameterReq <- parameterRequirements) yield parameterReq.toXml}
        </parameterRequirements>
      </config>
    </pluginInstance>

  def this(xmlNode: Node) = this(
    (xmlNode \ "pluginName").text,
    (xmlNode \ "pluginAuthor").text,
    (xmlNode \ "instanceName").text,
    for (node <- xmlNode \ "config" \ "sourceRequirements" \ "_") yield new SourceRequirementConfig(node),
    for (node <- xmlNode \ "config" \ "parameterRequirements" \ "_") yield new ParameterRequirementConfig(node))

}

case class SourceRequirementConfig(uniqueRequirementId: String, isInput: Boolean, sourceType: String, sourceId: String)
  extends Configuration {

  override def toXml: Elem =
    <sourceRequirement>
      <uniqueRequirementId>
        {uniqueRequirementId}
      </uniqueRequirementId>
      <isInput>
        {isInput}
      </isInput>
      <sourceType>
        {sourceType}
      </sourceType>
      <sourceId>
        {sourceId}
      </sourceId>
    </sourceRequirement>

  def this(xmlNode: Node) = this(
    (xmlNode \ "uniqueRequirementId").text,
    if ((xmlNode \ "isInput").text == "true") true else false,
    (xmlNode \ "sourceType").text,
    (xmlNode \ "sourceId").text)

}

case class ParameterRequirementConfig(uniqueRequirementId: String, value: String) extends Configuration {
  override def toXml: Elem =
    <parameterRequirement>
      <uniqueRequirementId>
        {uniqueRequirementId}
      </uniqueRequirementId>
      <value>
        {value}
      </value>
    </parameterRequirement>

  def this(xmlNode: Node) = this(
    (xmlNode \ "uniqueRequirementId").text,
    (xmlNode \ "value").text)

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