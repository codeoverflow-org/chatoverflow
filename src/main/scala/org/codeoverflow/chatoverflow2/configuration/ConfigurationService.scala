package org.codeoverflow.chatoverflow2.configuration

import java.io.File

import org.codeoverflow.chatoverflow2.WithLogger
import org.codeoverflow.chatoverflow2.connector.ConnectorRegistry
import org.codeoverflow.chatoverflow2.framework.PluginFramework
import org.codeoverflow.chatoverflow2.instance.PluginInstanceRegistry

import scala.xml.Node

/**
  * The configuration service provides methods to work with serialized state information.
  *
  * @param configFilePath the file path of the config file to work with
  */
class ConfigurationService(val configFilePath: String) extends WithLogger {
  private val defaultContent: Node =
    <config>
      <pluginInstances></pluginInstances>
      <connectorInstances></connectorInstances>
    </config>

  def loadPluginInstances(pluginInstanceRegistry: PluginInstanceRegistry,
                          pluginFramework: PluginFramework): Boolean = {
    try {
      val xmlContent = loadXML()

      for (node <- xmlContent \ "pluginInstances" \ "_") {
        val pluginName = (node \ "pluginName").text
        val pluginAuthor = (node \ "pluginAuthor").text
        val instanceName = (node \ "instanceName").text

        val pluginTypes = pluginFramework.getPlugins.
          filter(p => p.getName.equals(pluginName)).
          filter(p => p.getAuthor.equals(pluginAuthor))

        // Add plugin instance
        if (pluginTypes.length != 1) {
          logger info s"Unable to retrieve plugin type '$pluginName' ('$pluginAuthor')."
        } else {
          pluginInstanceRegistry.addPluginInstance(instanceName, pluginTypes.head)

          // Add requirements
          for (req <- node \ "requirements" \ "_") {
            val requirementId = (req \ "uniqueRequirementId").text
            val targetType = (req \ "targetType").text
            val content = (req \ "content").text

            logger info s"Loading requirement '$requirementId' of type '$targetType'."
            val instance = pluginInstanceRegistry.getPluginInstance(instanceName)

            if (instance.isEmpty) {
              // This should never happen
              logger error s"Unable to retrieve the just added plugin instance '$instanceName'."
            } else {
              val requirements = instance.get.getRequirements
              val requirement = requirements.getRequirementById(requirementId)

              // TODO: And now?
              // 1. Use the type registry and the target type to instantiate an element for the requirement
              // 2. Check if the types match with the requirement using instance of
              // 3. Find a way to set and deserialize the value. If there is no set-way, override it with the type information
              // 4. Refactor this to be in the plugin instance (probably)

            }
          }
        }



        // TODO: Add requirement support (use deserialize)
      }

      true
    } catch {
      case e: Exception =>
        logger error s"Unable to load plugin instances. An error occurred: ${e.getMessage}"
        false
    }
  }

  private def loadXML(): Node = {
    // TODO: Add some XML caching here
    if (!new File(configFilePath).exists()) {
      logger debug s"Config file '$configFilePath' not found. Initialising with default values."
      saveXML(defaultContent)
    }

    val xmlContent = xml.Utility.trim(xml.XML.loadFile(configFilePath))
    logger info "Loaded config file."
    xmlContent
  }

  private def saveXML(xmlContent: Node): Unit = {
    xml.XML.save(configFilePath, xmlContent)
    logger info "Saved config file."
  }

  def loadConnectors(credentialsService: CredentialsService): Boolean = {

    try {
      val xmlContent = loadXML()

      for (node <- xmlContent \ "connectorInstances" \ "_") {
        val sourceIdentifier = (node \ "sourceIdentifier").text
        val connectorType = (node \ "connectorType").text

        logger info s"Loaded connector '$sourceIdentifier' of type '$connectorType'."

        // Add connector to the registry
        if (ConnectorRegistry.addConnector(sourceIdentifier, connectorType)) {

          // Set credentials
          val credentials = credentialsService.get(connectorType, sourceIdentifier)
          if (credentials.isEmpty) {
            logger warn "No credentials found for this connector."
          } else {
            ConnectorRegistry.setConnectorCredentials(sourceIdentifier, connectorType, credentials.get)
            logger info "Successfully set credentials for this connector."
          }
        }
      }

      true
    } catch {
      case e: Exception =>
        logger error s"Unable to load Connectors. An error occurred: ${e.getMessage}"
        false
    }
  }

  /**
    * Tries to load the specified config file and fill all information in the public data objects.
    */
  def load(): Unit = {

    // Create file if non existent


    //val xmlContent =
    /*
        pluginInstances = for (node <- xmlContent \ "pluginInstances" \ "_") yield
          new PluginInstance(node)

        connectorInstances = for (node <- xmlContent \ "connectorInstances" \ "_") yield
          new ConnectorInstance(node)
    */
    // Insert new config options here

  }

  /**
    * Takes all config information from the public data object sequences and saves them to the specified config file.
    */
  def save(): Unit = {

    //val xmlContent = _
    /*=
      <config>
        <pluginInstances>
          {for (pluginInstance <- pluginInstances) yield pluginInstance.toXml}
        </pluginInstances>
        <connectorInstances>
          {for (connectorInstance <- connectorInstances) yield connectorInstance.toXml}
        </connectorInstances>
      </config>
*/
    // Insert new config options here

    //xml.XML.save(configFilePath, xmlContent)

  }

}