package org.codeoverflow.chatoverflow.ui.web.rest.config

import org.codeoverflow.chatoverflow.Launcher
import org.codeoverflow.chatoverflow.api.APIVersion
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.{ConfigInfo, Password, ResultMessage}
import org.scalatra.swagger._

class ConfigController(implicit val swagger: Swagger) extends JsonServlet with ConfigControllerDefinition {

  get("/", operation(getConfig)) {
    ConfigInfo("Chat Overflow", APIVersion.MAJOR_VERSION,
      APIVersion.MINOR_VERSION, chatOverflow.pluginFolderPath,
      chatOverflow.configFolderPath, chatOverflow.requirementPackage, Launcher.pluginDataPath)
  }

  post("/save", operation(postSave)) {
    chatOverflow.save()
    true
  }

  get("/login", operation(getLogin)) {
    chatOverflow.credentialsService.isLoggedIn
  }

  post("/login", operation(postLogin)) {
    parsedAs[Password] {
      case Password(password) =>
        chatOverflow.credentialsService.setPassword(password.toCharArray)
        if (!chatOverflow.load()) {
          // TODO: Could have more details with a more detailed loading process
          ResultMessage(success = false, "Unable to load. Wrong password?")
        } else {
          ResultMessage(success = true)
        }
    }
  }

  // TODO: Handle first time login / Creation of the config files (same as postLogin above...?)
}