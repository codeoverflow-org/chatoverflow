package org.codeoverflow.chatoverflow2

import java.security.Policy

import org.codeoverflow.chatoverflow.api.APIVersion
import org.codeoverflow.chatoverflow2.framework.security.SandboxSecurityPolicy

class ChatOverflow extends WithLogger {

  def init(): Unit = {
    logger info "Minzig!"
    logger debug s"Starting Chat Overflow Framework. " +
      s"API Version is ${APIVersion.MAJOR_VERSION}.${APIVersion.MINOR_VERSION}." +
      s"For more information and updates, please visit: http://codeoverflow.org"

    logger debug "Initialization started."

    logger debug "Enabling framework security policy."
    enableFrameworkSecurity()

    logger debug "Setup plugin framework."

  }

  def enableFrameworkSecurity(): Unit = {
    Policy.setPolicy(new SandboxSecurityPolicy)
    System.setSecurityManager(new SecurityManager)
  }

}
