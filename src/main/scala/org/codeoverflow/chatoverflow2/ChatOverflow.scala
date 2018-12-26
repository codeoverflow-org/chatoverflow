package org.codeoverflow.chatoverflow2

import java.security.Policy

import org.codeoverflow.chatoverflow.api.APIVersion
import org.codeoverflow.chatoverflow2.framework.PluginFramework
import org.codeoverflow.chatoverflow2.framework.security.SandboxSecurityPolicy
import org.codeoverflow.chatoverflow2.instance.PluginInstanceRegistry
import org.codeoverflow.chatoverflow2.requirement.RequirementTypeRegistry

/**
  * The chat overflow class is the heart of the project.
  * Here, all different parts of the software can be accessed and altered.
  *
  * @param pluginFolderPath the relative path ot the plugin folder ("plugins/" by default)
  */
class ChatOverflow(val pluginFolderPath: String = "plugins/",
                   val requirementPackage: String = "org.codeoverflow.chatoverflow2.requirement")
  extends WithLogger {

  val pluginFramework = new PluginFramework(pluginFolderPath)
  val pluginInstanceRegistry = new PluginInstanceRegistry
  val requirementTypeRegistry = new RequirementTypeRegistry(requirementPackage)

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

    logger debug "Scanning service requirement type definitions."
    requirementTypeRegistry.updateTypeRegistry()
    logger debug "Finished updating service requirements."

    logger debug "Initializing connector instance registry."
    // TODO: Connector Registry (should they also be derived by annotation?)


    // TODO: Credentials / Config / Requirements
  }

  private def enableFrameworkSecurity(): Unit = {
    Policy.setPolicy(new SandboxSecurityPolicy)
    System.setSecurityManager(new SecurityManager)
  }

}
