package org.codeoverflow.chatoverflow.configuration

import java.io.File

/**
  * The configuration service provides methods to work with serialized state information.
  *
  * @param configFilePath the file path of the config file to work with
  */
class ConfigurationService(val configFilePath: String) {

  /**
    * The list of all plugin instances. Make sure to load the config first.
    */
  var pluginInstances: Seq[PluginInstance] = Seq[PluginInstance]()

  /**
    * The list of all connector instances. Make sure to load the config first.
    */
  var connectorInstances: Seq[ConnectorInstance] = Seq[ConnectorInstance]()

  /**
    * Tries to load the specified config file and fill all information in the public data objects.
    */
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

  /**
    * Takes all config information from the public data object sequences and saves them to the specified config file.
    */
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