package org.codeoverflow.chatoverflow.ui.web.rest

import org.codeoverflow.chatoverflow.Launcher
import org.codeoverflow.chatoverflow.api.APIVersion
import org.codeoverflow.chatoverflow.ui.web.JsonServlet

class DefaultServlet extends JsonServlet {

  private val chatOverflow = Launcher.server.get.chatOverflow

  get("/") {
    DefaultInfo("Chat Overflow", APIVersion.MAJOR_VERSION,
      APIVersion.MINOR_VERSION, chatOverflow.pluginFolderPath,
      chatOverflow.configFolderPath, chatOverflow.requirementPackage)
  }

  case class DefaultInfo(name: String, apiMajorVersion: Int, apiMinorVersion: Int, pluginFolderPath: String,
                         configFolderPath: String, requirementPackage: String)

}
