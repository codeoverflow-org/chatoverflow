package org.codeoverflow.chatoverflow.ui.web.rest.config

import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.{ConfigInfo, Password, ResultMessage}
import org.scalatra.swagger.SwaggerSupport
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder

trait ConfigControllerDefinition extends SwaggerSupport {

  val getConfig: OperationBuilder =
    (apiOperation[ConfigInfo]("getConfig")
      summary "Shows general information and settings."
      description "Shows API version and chat overflow startup settings.")

  val postSave: OperationBuilder =
    (apiOperation[Boolean]("postSave")
      summary "Triggers the saving process of the framework manually."
      description "Triggers saving of credentials and configuration. Should not be needed manually.")

  val getLogin: OperationBuilder =
    (apiOperation[Boolean]("getLogin")
      summary "Returns if the framework is already supplied with a password to decrypt its credentials."
      description "Returns true, if a password has been set. Note that this says nothing about its correctness.")

  val postLogin: OperationBuilder =
    (apiOperation[ResultMessage]("postLogin")
      summary "Logs in the the framework with the given password, loads config and credentials."
      description "Tries to decrypt the credentials using the provided password. Does load configuration and credentials."
      parameter bodyParam[Password]("body").description("Requires the user framework password."))

  override protected def applicationDescription: String = "Handles configuration and information retrieval."
}
