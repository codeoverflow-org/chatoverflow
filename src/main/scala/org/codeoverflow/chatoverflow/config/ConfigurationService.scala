package org.codeoverflow.chatoverflow.config

import java.io.File

class ConfigurationService(val configDirectoryPath: String) {
  val configFile = s"$configDirectoryPath/config.xml"

  var pluginInstances: Seq[PluginInstance] = Seq[PluginInstance]()

  def load(): Unit = {

    // Create file if non existent
    if (!new File(configFile).exists()) {
      save()
    }

    val xmlContent = xml.Utility.trim(xml.XML.loadFile(configFile))

    pluginInstances = for (node <- xmlContent \ "pluginInstances" \ "_") yield
      new PluginInstance(node)

    // Insert new config options here

  }

  def save(): Unit = {

    val xmlContent =
      <config>
        <pluginInstances>
          {for (pluginInstance <- pluginInstances) yield pluginInstance.toXml}
        </pluginInstances>
      </config>

    // Insert new config options here

    xml.XML.save(configFile, xmlContent)

  }

}