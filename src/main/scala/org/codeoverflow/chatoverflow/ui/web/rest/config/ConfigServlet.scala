package org.codeoverflow.chatoverflow.ui.web.rest.config

import org.codeoverflow.chatoverflow.api.APIVersion
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.ConfigInfo
import org.scalatra.swagger._

class ConfigServlet(implicit val swagger: Swagger) extends JsonServlet with ConfigServletDefinition {

  get("/", operation(getConfig)) {
    response.setHeader("Access-Control-Allow-Origin", "*")
    ConfigInfo("Chat Overflow", APIVersion.MAJOR_VERSION,
      APIVersion.MINOR_VERSION, chatOverflow.pluginFolderPath,
      chatOverflow.configFolderPath, chatOverflow.requirementPackage)
  }
}