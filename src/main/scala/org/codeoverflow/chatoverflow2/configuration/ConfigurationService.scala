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

  /**
    * Reads the config xml file and creates plugin instances, saved in the instance registry.
    *
    * @param pluginInstanceRegistry the plugin registry to save the instances
    * @param pluginFramework        the plugin framework to retrieve plugin types
    * @param typeRegistry           the type registry to retrieve additional type information
    * @return false, if a general failure happened while loading, true if there were no or only minor errors
    */
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

            ConfigurationService.fulfillRequirementByDeserializing(instanceName, requirementId, targetType,
              content, pluginInstanceRegistry, typeRegistry)
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

  /**
    * Load all connector instances from the config xml and save them to the connector registry.
    *
    * @return false if a general failure happened, true if there were only minor or no errors
    */
  def loadConnectors(): Boolean = {

    try {
      val xmlContent = loadXML()

      for (node <- xmlContent \ "connectorInstances" \ "_") {
        val sourceIdentifier = (node \ "sourceIdentifier").text
        val connectorType = (node \ "connectorType").text

        logger info s"Loaded connector '$sourceIdentifier' of type '$connectorType'."

        // Add connector to the registry
        if (ConnectorRegistry.addConnector(sourceIdentifier, connectorType)) {
          logger info "Successfully added connector."
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
    * Loads the config xml file and return its content.
    */
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

  /**
    * Saves all connector and plugin instances.
    *
    * @param pluginInstanceRegistry the plugin instance registry to retrieve the instances from
    * @return false, if a major failure happened
    */
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
        logger error s"Unable to save configuration. Exception thrown: ${e.getMessage}"
        false
    }
  }

  /**
    * Saves the xml content to the config xml.
    */
  private def saveXML(xmlContent: Node): Unit = {
    xml.XML.save(configFilePath, xmlContent)
    logger info "Saved config file."
  }

  /**
    * Creates the xml for all plugin instances.
    */
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

  /**
    * Creates the xml for all requirements of a plugin.
    */
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
          {if (requirementMap.get(key).isSet) requirementMap.get(key).get().serialize()}
        </content>
      </requirement>
    }
  }

  /**
    * Creates the xml for a connector instance.
    */
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

object ConfigurationService extends WithLogger {

  /**
    * This function contains the more complex loading code needed to fulfill a requirement with
    * dynamically created content. A lot of information and access to different registries is needed.
    *
    * @param instanceName           the name of the plugin instance, should be loaded in the plugin instance registry
    * @param requirementId          the unique id of the requirement to fulfill
    * @param targetType             the target type of the fulfilling object, should be registered in the type registry
    * @param content                the serialized content that should be deserialized to the requirement
    * @param pluginInstanceRegistry the instantiated plugin instance registry in use
    * @param typeRegistry           the instantiated type registry in use
    * @return true, if this complex process worked fine. false in any other case
    */
  def fulfillRequirementByDeserializing(instanceName: String, requirementId: String,
                                        targetType: String, content: String,
                                        pluginInstanceRegistry: PluginInstanceRegistry,
                                        typeRegistry: TypeRegistry): Boolean = {
    // FIXME: Better handling of empty content (not set in the first place, then read from XML)

    logger info s"Loading requirement '$requirementId' of type '$targetType'."
    val instance = pluginInstanceRegistry.getPluginInstance(instanceName)

    if (instance.isEmpty) {
      // This should never happen
      logger error s"Unable to retrieve the just added plugin instance '$instanceName'."
      false
    } else {
      val requirements = instance.get.getRequirements
      val requirement = requirements.getRequirementById(requirementId)

      if (!requirement.isPresent) {
        logger error s"Unable to find requirement with the given id '$requirementId'."
        false
      } else {

        // Get the type of the requirement first
        val loadedRequirementType = typeRegistry.getRequirementImplementation(targetType)

        // Check if this type is an instance of the abstract requirement type
        if (loadedRequirementType.isEmpty) {
          logger error s"The loaded requirement type '$targetType' is not found."
          false
        } else {
          val requirementType = requirement.get.getTargetType

          if (!loadedRequirementType.get.getInterfaces.exists(reqType => reqType.getName.equals(requirementType.getName))) {
            logger error s"The loaded requirement type '${loadedRequirementType.get.getName}' is not compatible to '${requirementType.getName}'."
            false
          } else {
            logger info "Trying to instantiate the requirement content with the loaded value."

            try {
              val reqContent = loadedRequirementType.get.newInstance().asInstanceOf[io.Serializable]
              requirement.get.asInstanceOf[Requirement[io.Serializable]].set(reqContent)
              reqContent.deserialize(content)

              logger info s"Created requirement content for '$requirementId' and deserialized its content."
              true
            } catch {
              case e: Exception =>
                logger error s"Unable to instantiate requirement content. Exception: ${e.getMessage}"
                false
            }
          }
        }
      }
    }
  }

}