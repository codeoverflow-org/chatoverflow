package org.codeoverflow.chatoverflow

import java.io.File
import java.security.Policy

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.configuration._
import org.codeoverflow.chatoverflow.framework.{PluginFramework, PluginManagerImpl, SandboxSecurityPolicy}
import org.codeoverflow.chatoverflow.registry.{ConnectorRegistry, PluginInstanceRegistry, TypeRegistry}
import org.codeoverflow.chatoverflow.service.IO

object ChatOverflow {

  private val logger = Logger.getLogger(this.getClass)
  var pluginFolderPath = "plugins/"
  var configFolderPath = "config/"
  var configFilePath = s"$configFolderPath/config.xml"
  var credentialsFilePath = s"$configFolderPath/credentials"
  private var pluginFramework: PluginFramework = _
  private var pluginInstanceRegistry: PluginInstanceRegistry = _
  private var configurationService: ConfigurationService = _
  private var credentialsService: CredentialsService = _

  // TODO: Should be modeled and refactored again?

  /**
    * The init method initializes the complete framework. This includes configuration loading and dynamic type instantiation.
    * Also, all plugin jars are loaded here.
    */
  def preInit(): Unit = {
    println("Minzig!")
    logger info "Started Chat Overflow Framework. Hello everybody!"
    logger debug "INITIALIZATION STARTED!"

    // Initialize chat overflow plugin framework
    logger info "[1/6] Phase 1: Loading plugin framework and plugins."
    loadFramework()

    // Load configuration
    logger info "[2/6] Phase 2: Loading framework configuration."
    loadConfiguration()

    // Add all configured plugin instances to the plugin registry
    logger info "[3/6] Phase 3: Loading plugin instances."
    loadPluginInstances()

    // Load credentials for service login
    logger info "[4/6] Phase 4: Loading credentials."
    loadCredentials()

    logger debug "PRE INIT PHASE FINISHED."
  }

  private def loadCredentials(): Unit = {
    logger info s"Loading credentials from '$credentialsFilePath'."

    val password = scala.io.StdIn.readLine("Please enter password > ")

    credentialsService = new CredentialsService(credentialsFilePath, password.toCharArray)
    credentialsService.load()

    logger info "Finished loading."
  }

  private def loadAndSetRequirements(): Unit = {
    for (pluginInstance <- configurationService.pluginInstances) {
      logger info s"Loading requirements for instance '${pluginInstance.instanceName}'."

      val requirements = pluginInstanceRegistry.getRequirements(pluginInstance.instanceName)

      for (requirementConfig <- pluginInstance.requirements) {
        logger info s"Setting requirement '${requirementConfig.uniqueRequirementId}' of type '${requirementConfig.targetType}'."

        TypeRegistry.createRequirement(requirements, requirementConfig.targetType,
          requirementConfig.uniqueRequirementId, requirementConfig.serializedContent)
      }

      if (!requirements.allNeededRequirementsSet()) {
        logger warn s"Not all required Requirements for $pluginInstance had been set!"
      }
    }

    logger info "Finished loading."
  }

  private def loadFramework(): Unit = {
    // Create sandbox environment for plugins
    Policy.setPolicy(new SandboxSecurityPolicy)
    System.setSecurityManager(new SecurityManager)

    // Create plugin framework, registry and manager instance
    pluginInstanceRegistry = new PluginInstanceRegistry
    pluginFramework = PluginFramework(pluginFolderPath)
    pluginFramework.init(new PluginManagerImpl("INIT"))

    // Register types
    IO.registerTypes()
    logger info "Registered input / output / parameter types."

    logger debug "Finished loading."
  }

  private def loadPluginInstances(): Unit = {

    for (pluginInstance <- configurationService.pluginInstances) {

      val pluggable = pluginFramework.getPluggable(pluginInstance.pluginName, pluginInstance.pluginAuthor)

      pluggable match {
        case Some(p) =>
          logger info s"Loaded plugin config $pluginInstance."
          pluginInstanceRegistry.addPluginInstance(pluginInstance.instanceName, p)
        case None => logger debug s"Unable to load plugin config $pluginInstance. Plugin not found."
      }
    }

    logger info "Finished loading."
  }

  /**
    * The second init phase includes dynamic configuration. To work properly, the CLI must be checked first.
    */
  def postInit(): Unit = {
    logger debug "STARTED POST INITIALIZATION."

    // Load all connectors with the given credentials
    logger info "[5/6] Phase 5: Loading platform connectors with given credentials."
    loadConnectors()

    // Load plugin instance configuration (e.g. specified target platforms)
    logger info "[6/6] Load plugin instance configuration."
    loadAndSetRequirements()

    logger debug "INITIALIZATION FINISHED!"
  }

  private def loadConfiguration(): Unit = {

    logger info s"Loading credentials from '$configFilePath'."


    if (!new File(configFolderPath).exists()) {
      new File(configFolderPath).mkdir()
    }

    configurationService = new ConfigurationService(configFilePath)
    configurationService.load()

    logger info "Finished loading."
  }

  private def loadConnectors(): Unit = {

    for (connectorInstance <- configurationService.connectorInstances) {

      logger info s"Loading connector of type '${connectorInstance.connectorType}' " +
        s"for source '${connectorInstance.sourceIdentifier}'."

      val credentials = credentialsService.getCredentials(connectorInstance.connectorType, connectorInstance.sourceIdentifier)

      ConnectorRegistry.addConnector(connectorInstance.connectorType, connectorInstance.sourceIdentifier, credentials)
    }

    logger info "Finished loading."
  }

  def addPluginInstance(pluginName: String, pluginAuthor: String, instanceName: String): Unit = {
    configurationService.pluginInstances = configurationService.pluginInstances ++
      Seq(PluginInstance(pluginName, pluginAuthor, instanceName, Seq[RequirementConfig]()))
    logger info s"Added plugin instance '$instanceName'."
    configurationService.save()
  }

  def addConnector(connectorType: String, sourceIdentifier: String): Unit = {
    configurationService.connectorInstances = configurationService.connectorInstances ++
      Seq(ConnectorInstance(connectorType, sourceIdentifier))
    logger info s"Added connector '$sourceIdentifier'."
    configurationService.save()
  }

  def addCredentials(credentialsType: String, credentialsIdentifier: String): Unit = {
    val c = new Credentials(credentialsIdentifier)
    if (credentialsService.existCredentials(credentialsType, credentialsIdentifier)) {
      logger warn s"Credentials of type '$credentialsType' for '$credentialsIdentifier' do already exist."
    } else {
      credentialsService.addCredentials(credentialsType, c)
      logger info s"Added credentials for '$credentialsIdentifier'."
      credentialsService.save()
    }
  }

  def addCredentialsEntry(credentialsType: String, credentialsIdentifier: String, key: String, value: String): Unit = {
    val c = credentialsService.getCredentials(credentialsType, credentialsIdentifier)

    if (c.isEmpty) {
      logger warn s"Credentials of type '$credentialsType' for '$credentialsIdentifier' do not exist."
    } else {
      if (c.get.exists(key)) {
        logger warn "In '$credentialsIdentifier', a value named '$key' is already defined."
      } else {
        c.get.addValue(key, value)
        logger info s"Added credentials entry '$key' to '$credentialsIdentifier'."
      }
    }
    credentialsService.save()
  }

  def addRequirement(instanceName: String, uniqueId: String, requirementType: String, serializedContent: String): Unit = {
    val instances = configurationService.pluginInstances.filter(_.instanceName == instanceName)

    if (instances.length != 1) {
      logger warn s"Unable to find the specified plugin instance '$instanceName'."
    } else {

      // Remove old entry first
      instances.head.requirements = instances.head.requirements.filter(_.uniqueRequirementId != uniqueId)

      instances.head.requirements = instances.head.requirements ++
        Seq(RequirementConfig(uniqueId, requirementType, serializedContent))

      logger info s"Added requirement '$uniqueId' to plugin instance '$instanceName'."
      configurationService.save()
    }
  }

  def startPlugin(instanceName: String): Unit = {
    pluginInstanceRegistry.asyncStartPlugin(instanceName)
  }
}
