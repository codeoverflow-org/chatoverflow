package org.codeoverflow.chatoverflow2

import java.security.Policy

import org.codeoverflow.chatoverflow.api.APIVersion
import org.codeoverflow.chatoverflow2.configuration.{ConfigurationService, CredentialsService}
import org.codeoverflow.chatoverflow2.connector.ConnectorRegistry
import org.codeoverflow.chatoverflow2.framework.PluginFramework
import org.codeoverflow.chatoverflow2.framework.security.SandboxSecurityPolicy
import org.codeoverflow.chatoverflow2.instance.PluginInstanceRegistry
import org.codeoverflow.chatoverflow2.registry.TypeRegistry

/**
  * The chat overflow class is the heart of the project.
  * Here, all different parts of the software can be accessed and altered.
  *
  * @param pluginFolderPath   the relative path ot the plugin folder ("plugins/" by default)
  * @param configFolderPath   the relative path of the config folder ("config/" by default)
  * @param requirementPackage the fully qualified name of the requirement package
  */
class ChatOverflow(val pluginFolderPath: String = "plugins/",
                   val configFolderPath: String = "config/",
                   val requirementPackage: String = "org.codeoverflow.chatoverflow2.requirement")
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
    // TODO: Go trough the paper and check if all requirements for chat overflow are met already
    // TODO: Credentials / Config / Requirements
    // To safe: connector registry, credentials (from credentials provider), plugin instance registry with requirements
    // SAVE:
    // credentialsService.save()
    // configService.saveConnectors()
    // configService.savePluginInstances(pluginInstanceRegistry)
    // LOAD:
    // credentialsService.load()                                    DONE
    // configService.loadConnectors()                               DONE
    // configService.loadPluginInstances(pluginInstanceRegistry)    WORKING...
    // TODO: Update REPL to be able to do do... more.
  }

  private def askForPassword(): Unit = {
    val password = scala.io.StdIn.readLine("Please enter password > ").toCharArray
    credentialsService.setPassword(password)
  }

  private def enableFrameworkSecurity(): Unit = {
    Policy.setPolicy(new SandboxSecurityPolicy)
    System.setSecurityManager(new SecurityManager)
  }

  def load(): Unit = {
    // Start by loading credentials
    credentialsService.load()

    // Load connector instances and plugin instances
    configService.loadConnectors(credentialsService)
    configService.loadPluginInstances(pluginInstanceRegistry, pluginFramework)
  }

  def save(): Unit = {

  }

}
