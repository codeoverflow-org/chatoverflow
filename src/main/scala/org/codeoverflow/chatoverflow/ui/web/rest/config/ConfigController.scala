package org.codeoverflow.chatoverflow.ui.web.rest.config

import org.codeoverflow.chatoverflow.api.APIVersion
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.ConfigInfo
import org.scalatra.swagger._

class ConfigController(implicit val swagger: Swagger) extends JsonServlet with ConfigControllerDefinition {

  get("/", operation(getConfig)) {
    ConfigInfo("Chat Overflow", APIVersion.MAJOR_VERSION,
      APIVersion.MINOR_VERSION, chatOverflow.pluginFolderPath,
      chatOverflow.configFolderPath, chatOverflow.requirementPackage)
  }
}