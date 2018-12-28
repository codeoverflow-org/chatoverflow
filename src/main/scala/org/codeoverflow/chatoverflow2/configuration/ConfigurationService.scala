package org.codeoverflow.chatoverflow2.configuration

import java.io.File

import org.codeoverflow.chatoverflow.api.io
import org.codeoverflow.chatoverflow.api.plugin.configuration.{Requirement, Requirements}
import org.codeoverflow.chatoverflow2.WithLogger
import org.codeoverflow.chatoverflow2.connector.ConnectorRegistry
import org.codeoverflow.chatoverflow2.framework.PluginFramework
import org.codeoverflow.chatoverflow2.instance.PluginInstanceRegistry
import org.codeoverflow.chatoverflow2.registry.TypeRegistry

import scala.xml.{Elem, Node}

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
                          pluginFramework: PluginFramework,
                          typeRegistry: TypeRegistry): Boolean = {
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

              if (!requirement.isPresent) {
                logger error s"Unable to find requirement with the given id '$requirementId'."
              } else {

                // Get the type of the requirement first
                val loadedRequirementType = typeRegistry.getRequirementImplementation(targetType)

                // Check if this type is an instance of the abstract requirement type
                if (loadedRequirementType.isEmpty) {
                  logger error s"The loaded requirement type '$targetType' is not found."
                } else {
                  val requirementType = requirement.get.getTargetType

                  if (!loadedRequirementType.get.getInterfaces.exists(reqType => reqType.getName.equals(requirementType.getName))) {
                    logger error s"The loaded requirement type '${loadedRequirementType.get.getName}' is not compatible to '${requirementType.getName}'."
                  } else {
                    logger info "Trying to instantiate the requirement content with the loaded value."

                    try {
                      val reqContent = loadedRequirementType.get.newInstance().asInstanceOf[io.Serializable]
                      requirement.get.asInstanceOf[Requirement[io.Serializable]].set(reqContent)
                      reqContent.deserialize(content)

                      logger info s"Created requirement content for '$requirementId' and deserialized its content."
                    } catch {
                      case e: Exception => logger error s"Unable to instantiate requirement content. Exception: ${e.getMessage}"
                    }
                  }
                }
              }
            }
          }
        }
      }

      true
    } catch {
      case e: Exception =>
        logger error s"Unable to load plugin instances. An error occurred: ${e.getMessage}"
        false
    }
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

  def save(pluginInstanceRegistry: PluginInstanceRegistry): Boolean = {
    logger info "Started saving current configuration."
    try {
      val pluginXML = createPluginInstanceXML(pluginInstanceRegistry)
      val connectorXML = createConnectorInstanceXML()

      val xmlContent =
        <config>
          <pluginInstances>
            {pluginXML}
          </pluginInstances>
          <connectorInstances>
            {connectorXML}
          </connectorInstances>
        </config>

      saveXML(xmlContent)
      true
    } catch {
      case e: Exception =>
        logger error s"Unable to save configuration. Exception thronw: ${e.getMessage}"
        false
    }
  }

  private def createPluginInstanceXML(pluginInstanceRegistry: PluginInstanceRegistry): List[Elem] = {
    val pluginInstances = pluginInstanceRegistry.getAllPluginInstances

    for (instance <- pluginInstances) yield {
      <pluginInstance>
        <pluginName>
          {instance.getPluginTypeName}
        </pluginName>
        <pluginAuthor>
          {instance.getPluginTypeAuthor}
        </pluginAuthor>
        <instanceName>
          {instance.instanceName}
        </instanceName>
        <requirements>
          {createRequirementXML(instance.getRequirements)}
        </requirements>
      </pluginInstance>
    }
  }

  private def createRequirementXML(requirements: Requirements): Array[Elem] = {
    val requirementMap = requirements.getRequirementMap
    val keys = requirementMap.keySet().toArray

    for (key <- keys) yield {
      <requirement>
        <uniqueRequirementId>
          {key}
        </uniqueRequirementId>
        <targetType>
          {requirementMap.get(key).getTargetType.getName}
        </targetType>
        <content>
          {requirementMap.get(key).get().serialize()}
        </content>
      </requirement>
    }
  }

  private def createConnectorInstanceXML(): List[Elem] = {
    for (connectorKey <- ConnectorRegistry.getConnectorKeys) yield {
      <connectorInstance>
        <connectorType>
          {connectorKey.qualifiedConnectorName}
        </connectorType>
        <sourceIdentifier>
          {connectorKey.sourceIdentifier}
        </sourceIdentifier>
      </connectorInstance>
    }
  }

}