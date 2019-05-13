package org.codeoverflow.chatoverflow.ui.web.rest.config

import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.ConfigInfo
import org.scalatra.swagger.SwaggerSupport
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder

trait ConfigServletDefinition extends SwaggerSupport {

  val getConfig: OperationBuilder =
    (apiOperation[ConfigInfo]("getConfig")
      summary "Shows general information and settings."
      description "Shows API version and chat overflow startup settings.")

  override protected def applicationDescription: String = "Handles configuration and information retrieval."
}
