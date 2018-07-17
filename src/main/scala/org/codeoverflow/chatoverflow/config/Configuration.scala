package org.codeoverflow.chatoverflow.config

import java.io.File

class Configuration(val configDirectoryPath: String) {
  val configFile = s"$configDirectoryPath/config.xml"

  var pluginInstances: Seq[PluginInstanceConfigEntry] = Seq[PluginInstanceConfigEntry]()

  def load(): Unit = {

    // Create file if non existent
    if (!new File(configFile).exists()) {
      save()
    }

    val xmlContent = xml.Utility.trim(xml.XML.loadFile(configFile))

    pluginInstances = for (pluginInstance <- xmlContent \ "pluginInstances" \ "_") yield
      new PluginInstanceConfigEntry(pluginInstance)

  }

  def save(): Unit = {

    val xmlContent =
      <config>
        <pluginInstances>
          {for (pluginInstance <- pluginInstances) yield pluginInstance.toXml}
        </pluginInstances>
      </config>

    xml.XML.save(configFile, xmlContent)

  }

}