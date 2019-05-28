package org.codeoverflow.chatoverflow.ui.web.rest.config

import org.codeoverflow.chatoverflow.Launcher
import org.codeoverflow.chatoverflow.api.APIVersion
import org.codeoverflow.chatoverflow.configuration.CryptoUtil
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.{AuthKey, ConfigInfo, Password, ResultMessage}
import org.scalatra.swagger._

class ConfigController(implicit val swagger: Swagger) extends JsonServlet with ConfigControllerDefinition {

  get("/", operation(getConfig)) {
    ConfigInfo("Chat Overflow", APIVersion.MAJOR_VERSION,
      APIVersion.MINOR_VERSION, chatOverflow.pluginFolderPath,
      chatOverflow.configFolderPath, chatOverflow.requirementPackage, Launcher.pluginDataPath)
  }

  post("/save", operation(postSave)) {
    if (!chatOverflow.isLoaded) {
      false
    } else {
      chatOverflow.save()
      true
    }
  }

  // Is this even a thing?
  post("/exit", operation(postExit)) {
    parsedAs[AuthKey] {
      case AuthKey(authKey) =>
        if (authKey != chatOverflow.credentialsService.generateAuthKey()) {
          ResultMessage(success = false, "Wrong auth key.")

        } else {
          // Give enough time to return success. Then bye bye
          new Thread(() => {
            Thread.sleep(500)
            System.exit(0)
          }).start()
          ResultMessage(success = true)
        }
    }
  }

  get("/login", operation(getLogin)) {
    chatOverflow.isLoaded
  }

  post("/login", operation(postLogin)) {
    parsedAs[Password] {
      case Password(password) =>

        // Check for password correctness first
        if (!chatOverflow.credentialsService.checkPasswordCorrectness(password.toCharArray)) {
          ResultMessage(success = false, "Unable to login. Wrong password!")
        } else {

          // Password is correct. Login if first call.
          if (!chatOverflow.isLoaded) {
            chatOverflow.credentialsService.setPassword(password.toCharArray)
            if (!chatOverflow.load()) {
              ResultMessage(success = false, "Unable to load.")
            } else {

              // "Loading was successful, here is your auth key based on your supplied password"
              ResultMessage(success = true, CryptoUtil.generateAuthKey(password))
            }
          } else {
            // "Loading was not needed, but here is your key based on your password"
            ResultMessage(success = true, CryptoUtil.generateAuthKey(password))
          }
        }
    }
  }

  get("/register", operation(getRegister)) {
    chatOverflow.credentialsService.credentialsFileExists()
  }

  post("/register", operation(postRegister)) {
    parsedAs[Password] {
      case Password(password) =>

        if (chatOverflow.credentialsService.credentialsFileExists()) {
          ResultMessage(success = false, "Already registered.")

          // Extreme exotic case. Wtf?
        } else if (chatOverflow.isLoaded) {
          ResultMessage(success = false, "Framework already loaded.")

        } else {
          chatOverflow.credentialsService.setPassword(password.toCharArray)

          if (!chatOverflow.load()) {
            ResultMessage(success = false, "Unable to load.")
          } else {

            ResultMessage(success = true, CryptoUtil.generateAuthKey(password))
          }
        }
    }
  }
}