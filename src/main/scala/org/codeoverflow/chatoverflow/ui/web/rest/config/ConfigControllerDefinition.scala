package org.codeoverflow.chatoverflow.ui.web.rest.config

import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.{AuthKey, ConfigInfo, Password, ResultMessage}
import org.scalatra.swagger.SwaggerSupport
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder

trait ConfigControllerDefinition extends SwaggerSupport {

  val getConfig: OperationBuilder =
    (apiOperation[ConfigInfo]("getConfig")
      summary "Shows general information and settings."
      description "Shows API version and chat overflow startup settings.")

  val postSave: OperationBuilder =
    (apiOperation[Boolean]("postSave")
      summary "Triggers the saving process of the framework manually (if loaded previously)."
      description "Triggers saving of credentials and configuration. Should not be needed manually.")

  val postExit: OperationBuilder =
    (apiOperation[ResultMessage]("postExit")
      summary "Shuts the framework down."
      description "Shutdown the framework in the next second if a correct auth key is supplied."
      parameter bodyParam[AuthKey]("body").description("Requires the run specific auth key."))


  val getLogin: OperationBuilder =
    (apiOperation[Boolean]("getLogin")
      summary "Returns if the framework is already loaded."
      description "Returns true, if the framework had been loaded previously with success.")

  val postLogin: OperationBuilder =
    (apiOperation[ResultMessage]("postLogin")
      summary "Logs in the the framework with the given password, loads config and credentials."
      description "Tries to decrypt the credentials using the provided password. If already loaded, does only return the communication auth key."
      parameter bodyParam[Password]("body").description("Requires the user framework password. The auth key is based on this input."))

  val getRegister: OperationBuilder =
    (apiOperation[Boolean]("getRegister")
      summary "Returns if a credentials file has been created and thus a specific password been registered."
      description "Returns true, if the credentials file (with set password) already exists.")

  val postRegister: OperationBuilder =
    (apiOperation[ResultMessage]("postRegister")
      summary "Registers the user with the given password. Can only be called if there is no credentials file."
      description "Creates a credentials file with the given password. Acts like post(login) after this (returning an auth key)."
      parameter bodyParam[Password]("body").description("Requires the user framework password. The auth key is based on this input."))


  override protected def applicationDescription: String = "Handles configuration and information retrieval."
}
