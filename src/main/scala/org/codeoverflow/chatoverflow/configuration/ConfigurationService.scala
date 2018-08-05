package org.codeoverflow.chatoverflow.configuration

import java.io.File

class ConfigurationService(val configFilePath: String) {

  var pluginInstances: Seq[PluginInstance] = Seq[PluginInstance]()
  var connectorInstances: Seq[ConnectorInstance] = Seq[ConnectorInstance]()

  def load(): Unit = {

    // Create file if non existent
    if (!new File(configFilePath).exists()) {
      save()
    }

    val xmlContent = xml.Utility.trim(xml.XML.loadFile(configFilePath))

    pluginInstances = for (node <- xmlContent \ "pluginInstances" \ "_") yield
      new PluginInstance(node)

    connectorInstances = for (node <- xmlContent \ "connectorInstances" \ "_") yield
      new ConnectorInstance(node)

    // Insert new config options here

  }

  def save(): Unit = {

    val xmlContent =
      <config>
        <pluginInstances>
          {for (pluginInstance <- pluginInstances) yield pluginInstance.toXml}
        </pluginInstances>
        <connectorInstances>
          {for (connectorInstance <- connectorInstances) yield connectorInstance.toXml}
        </connectorInstances>
      </config>

    // Insert new config options here

    xml.XML.save(configFilePath, xmlContent)

  }

}