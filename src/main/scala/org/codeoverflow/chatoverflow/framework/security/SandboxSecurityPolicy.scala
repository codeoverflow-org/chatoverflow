package org.codeoverflow.chatoverflow.framework.security

import java.security._

import org.codeoverflow.chatoverflow.framework.helper.PluginClassLoader

/**
  * The Sandbox Policy provides different permission sets for plugins and application code.
  */
class SandboxSecurityPolicy extends Policy {

  /**
    * Checks if the domain is application or plugin, returns all or no permissions
    *
    * @param domain the domain of the managed element
    * @return either the permission set for plugins or the application
    */
  override def getPermissions(domain: ProtectionDomain): PermissionCollection =
    if (isPlugin(domain))
      applicationPermissions // TODO pluginPermissions (when using actors!)
    else
      applicationPermissions

  private def isPlugin(domain: ProtectionDomain) = domain.getClassLoader.isInstanceOf[PluginClassLoader]

  /**
    * Plugins do not have any permissions.
    */
  private def pluginPermissions: Permissions = new Permissions()

  /**
    * The application has all permissions.
    */
  private def applicationPermissions: Permissions = {
    val permissions = new Permissions()
    permissions.add(new AllPermission())
    permissions
  }

}
