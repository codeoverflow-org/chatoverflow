package org.codeoverflow.chatoverflow.ui.web.rest

import org.codeoverflow.chatoverflow.api.APIVersion
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.ConfigInfo

class ConfigServlet extends JsonServlet {

  get("/") {
    ConfigInfo("Chat Overflow", APIVersion.MAJOR_VERSION,
      APIVersion.MINOR_VERSION, chatOverflow.pluginFolderPath,
      chatOverflow.configFolderPath, chatOverflow.requirementPackage)
  }

}
