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
  private var configFolderPath = "config/"
  var configFilePath = s"$configFolderPath/config.xml"
  var credentialsFilePath = s"$configFolderPath/credentials.xml"

  private var pluginFramework: PluginFramework = _
  private var pluginRegistry: PluginInstanceRegistry = _
  private var configurationService: ConfigurationService = _
  private var credentialsService: CredentialsService = _

  def init(): Unit = {
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
    // TODO: Add method to add instance by console

    // Load credentials for service login
    logger info "[4/6] Phase 4: Loading credentials."
    loadCredentials()
    // TODO: Add method to add credentials by console

    // Load all connectors with the given credentials
    logger info "[5/6] Phase 5: Loading platform connectors with given credentials."
    loadConnectors()
    // TODO: Add method to add controllers by console

    // Load plugin instance configuration (e.g. specified target platforms)
    logger info "[6/6] Load plugin instance configuration."
    loadAndSetRequirements()
    // TODO: Add method to add configs by console

    logger debug "INITIALIZATION FINISHED!"

    /*
        configurationService.pluginInstances.head.parameterRequirements = Seq(ParameterRequirementConfig("helloReq", "This is skate!"))
        configurationService.pluginInstances.head.sourceRequirements =
          Seq(SourceRequirementConfig("input", isInput = true, "org.codeoverflow.chatoverflow.service.twitch.impl.TwitchChatInputImpl", "skate702"))
        configurationService.save()
    */
    //pluginRegistry.getRequirements("myfirstinstance").addInputRequirement()

    /*
    configurationService.pluginInstances = Seq[PluginInstance](PluginInstance("simpletest", "sebinside", "myfirstinstance"))
    val c = new Credentials("skate702")
    c.addValue("ouath", "oauth:xxx")
    val t = new TwitchConnector("skate702", c)
    credentialsService.addCredentials(t.getUniqueTypeString, c)
    configurationService.connectorInstances = Seq[ConnectorInstance](ConnectorInstance(t.getUniqueTypeString, "skate702"))

    credentialsService.save()
    configurationService.save()


    configurationService.pluginInstances.head.requirements = Seq(
      RequirementConfig("reqTwitch", "A twitch channel", false,
        "org.codeoverflow.chatoverflow.api.io.input.chat.TwitchChatInput", "skate702"),
      RequirementConfig("reqHello", "Your name", true, "java.lang.String", "seb"))

    configurationService.save()



*/


    // TODO: Write console stuff for setting configs (Updating config, updating running system?)
    // TODO: Encryption for credentials
    // TODO: Write documentation
    // TODO: Write wiki for new connector types
    // TODO: Write wiki for new plugins
  }

  private def loadAndSetRequirements(): Unit = {
    for (pluginInstance <- configurationService.pluginInstances) {
      logger info s"Loading requirements for instance '${pluginInstance.instanceName}'."

      val requirements = pluginRegistry.getRequirements(pluginInstance.instanceName)

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
    val pluginManager = new PluginManagerImpl
    pluginRegistry = new PluginInstanceRegistry(pluginManager)
    pluginFramework = PluginFramework(pluginFolderPath)
    pluginFramework.init(pluginManager)

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
          pluginRegistry.addPluginInstance(pluginInstance.instanceName, p)
        case None => logger debug s"Unable to load plugin config $pluginInstance. Plugin not found."
      }
    }

    logger info "Finished loading."
  }

  private def loadCredentials(): Unit = {
    // TODO: Ask for real password!

    logger info s"Loading credentials from '$credentialsFilePath'."

    credentialsService = new CredentialsService(credentialsFilePath, "kappa123".toCharArray)
    credentialsService.load()

    logger info "Finished loading."
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

  def startPlugin(instanceName: String): Unit = {
    pluginRegistry.asyncStartPlugin(instanceName)
  }
}
