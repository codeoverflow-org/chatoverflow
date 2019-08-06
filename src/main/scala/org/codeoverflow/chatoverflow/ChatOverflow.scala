package org.codeoverflow.chatoverflow

import java.io.File
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
                   val requirementPackage: String,
                   val logOutputOnConsole: Boolean)
  extends WithLogger {

  val pluginFramework = new PluginFramework(pluginFolderPath)
  val pluginInstanceRegistry = new PluginInstanceRegistry(logOutputOnConsole)
  val typeRegistry = new TypeRegistry(requirementPackage)
  val credentialsService = new CredentialsService(s"$configFolderPath/credentials")
  val configService = new ConfigurationService(s"$configFolderPath/config.xml")
  private var loaded = false

  /**
    * Returns if configs and credentials had been loaded.
    *
    * @return true, if has been called successfully in this run of the framework.
    */
  def isLoaded: Boolean = loaded

  /**
    * Initializes all parts of chat overflow. These can be accessed trough the public variables.
    */
  def init(): Unit = {
    logger info "Minzig!"
    logger debug s"Starting Chat Overflow Framework. " +
      s"API Version is '${APIVersion.MAJOR_VERSION}.${APIVersion.MINOR_VERSION}'. " +
      s"For more information and updates, please visit: http://codeoverflow.org"

    logger debug "Initialization started."

    logger debug "Ensuring that all required directories exist."
    createDirectories()

    logger debug "Enabling framework security policy."
    enableFrameworkSecurity()

    logger debug "Starting plugin framework."
    pluginFramework.loadNewPlugins()
    logger debug "Finished plugin framework initialization."

    logger debug "Scanning service requirement and connector type definitions."
    typeRegistry.updateTypeRegistry()
    ConnectorRegistry.setTypeRegistry(typeRegistry)
    logger debug "Finished updating type registry."

    logger debug "Finished initialization."
  }

  /**
    * Loads all config settings and credentials from the config folder.
    * Note that its only possible once per run to load everything successfully.
    */
  def load(): Boolean = {
    if (loaded) {
      false
    } else {
      val currentTime = System.currentTimeMillis()
      var success = true

      // Start by loading connectors
      if (!configService.loadConnectors())
        success = false

      // Then load credentials that can be put into the connectors
      if (success && !credentialsService.load())
        success = false

      // Finish by loading plugin instances
      if (success && !configService.loadPluginInstances(pluginInstanceRegistry, pluginFramework, typeRegistry))
        success = false

      logger info s"Loading took ${System.currentTimeMillis() - currentTime} ms."

      loaded = success
      success
    }
  }

  private def enableFrameworkSecurity(): Unit = {
    Policy.setPolicy(new SandboxSecurityPolicy)
    System.setSecurityManager(new SecurityManager)
  }

  private def createDirectories(): Unit = {
    Set(pluginFolderPath, configFolderPath, Launcher.pluginDataPath)
      .foreach(path => new File(path).mkdir())
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
