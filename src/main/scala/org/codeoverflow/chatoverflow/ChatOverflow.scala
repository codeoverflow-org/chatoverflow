package org.codeoverflow.chatoverflow

import java.security.Policy

import org.codeoverflow.chatoverflow.api.APIVersion
import org.codeoverflow.chatoverflow.configuration.{ConfigurationService, CredentialsService}
import org.codeoverflow.chatoverflow.connector.ConnectorRegistry
import org.codeoverflow.chatoverflow.framework.PluginFramework
import org.codeoverflow.chatoverflow.framework.security.SandboxSecurityPolicy
import org.codeoverflow.chatoverflow.instance.PluginInstanceRegistry
import org.codeoverflow.chatoverflow.registry.TypeRegistry

/**
  * The chat overflow class is the heart of the project.
  * Here, all different parts of the software can be accessed and altered.
  *
  * @param pluginFolderPath   the relative path ot the plugin folder ("plugins/" by default)
  * @param configFolderPath   the relative path of the config folder ("config/" by default)
  * @param requirementPackage the fully qualified name of the requirement package
  */
class ChatOverflow(val pluginFolderPath: String,
                   val configFolderPath: String,
                   val requirementPackage: String)
  extends WithLogger {

  val pluginFramework = new PluginFramework(pluginFolderPath)
  val pluginInstanceRegistry = new PluginInstanceRegistry
  val typeRegistry = new TypeRegistry(requirementPackage)
  val credentialsService = new CredentialsService(s"$configFolderPath/credentials")
  val configService = new ConfigurationService(s"$configFolderPath/config.xml")

  /**
    * Initializes all parts of chat overflow. These can be accessed trough the public variables.
    */
  def init(): Unit = {
    logger info "Minzig!"
    logger debug s"Starting Chat Overflow Framework. " +
      s"API Version is '${APIVersion.MAJOR_VERSION}.${APIVersion.MINOR_VERSION}'. " +
      s"For more information and updates, please visit: http://codeoverflow.org"

    logger debug "Initialization started."

    logger debug "Enabling framework security policy."
    enableFrameworkSecurity()

    logger debug "Starting plugin framework."
    pluginFramework.loadNewPlugins()
    logger debug "Finished plugin framework initialization."

    logger debug "Scanning service requirement and connector type definitions."
    typeRegistry.updateTypeRegistry()
    ConnectorRegistry.setTypeRegistry(typeRegistry)
    logger debug "Finished updating type registry."

    logger debug "Loading configs and credentials."
    askForPassword()
    load()
    logger debug "Finished loading configs and credentials."

    logger debug "Finished initialization."
  }

  private def askForPassword(): Unit = {
    val password = if (System.console() != null)
        System.console().readPassword("Please enter password (Input hidden) > ")
      else
        scala.io.StdIn.readLine("Please enter password (Input NOT hidden) > ").toCharArray
    credentialsService.setPassword(password)
  }

  private def enableFrameworkSecurity(): Unit = {
    Policy.setPolicy(new SandboxSecurityPolicy)
    System.setSecurityManager(new SecurityManager)
  }

  /**
    * Loads all config settings and credentials from the config folder.
    */
  def load(): Unit = {
    val currentTime = System.currentTimeMillis()

    // Start by loading connectors
    configService.loadConnectors()

    // Then load credentials that can be put into the connectors
    credentialsService.load()

    // Finish by loading plugin instances
    configService.loadPluginInstances(pluginInstanceRegistry, pluginFramework, typeRegistry)

    logger info s"Loading took ${System.currentTimeMillis() - currentTime} ms."
  }

  /**
    * Saves all settings and credentials to the corresponding files in the config folder.
    */
  def save(): Unit = {
    val currentTime = System.currentTimeMillis()

    // Start by saving credentials
    credentialsService.save()

    // Save connectors and plugin instances (Note: Less work then loading)
    configService.save(pluginInstanceRegistry)

    logger info s"Saving took ${System.currentTimeMillis() - currentTime} ms."
  }

}
