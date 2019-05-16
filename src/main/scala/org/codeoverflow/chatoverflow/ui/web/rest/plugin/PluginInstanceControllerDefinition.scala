package org.codeoverflow.chatoverflow.ui.web.rest.plugin

import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.{PluginInstance, PluginInstanceRef, Requirement, ResultMessage}
import org.scalatra.swagger.SwaggerSupport
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder

trait PluginInstanceControllerDefinition extends SwaggerSupport {

  val getInstances: OperationBuilder =
    (apiOperation[List[PluginInstance]]("getInstances")
      summary "Shows all plugin instances."
      description "Shows the name, plugin information and requirement keys of all plugin instances.")

  val getInstance: OperationBuilder =
    (apiOperation[PluginInstance]("getInstance")
      summary "Shows a plugin instance."
      description "Shows the name, plugin information and requirement keys of a specified plugin instance."
      parameter pathParam[String]("instanceName").description("The name of the plugin instance."))

  val getRequirements: OperationBuilder =
    (apiOperation[List[Requirement]]("getRequirements")
      summary "Shows all requirements of a plugin instance."
      description "Shows state, name and type of all requirements of a specified plugin instance."
      parameter pathParam[String]("instanceName").description("The name of the plugin instance."))

  val getLog: OperationBuilder =
    (apiOperation[List[String]]("getLog")
      summary "Shows the log of a plugin instance."
      description "Shows all (or the newest) log messages of a specified plugin instance."
      parameter pathParam[String]("instanceName").description("The name of the plugin instance.")
      parameter queryParam[Option[String]]("startIndex").description("The start index of the message stream").optional)

  val postInstance: OperationBuilder =
    (apiOperation[ResultMessage]("postInstance")
      summary "Creates a new plugin instance."
      description "Creates a new plugin instance with given name and plugin type."
      parameter bodyParam[PluginInstanceRef]("body").description("Requires new instance name and PluginType (name and author)."))

  val deleteInstance: OperationBuilder =
    (apiOperation[PluginInstance]("deleteInstance")
      summary "Removes a plugin instance."
      description "Removes a plugin instance specified by its name, if possible."
      parameter pathParam[String]("instanceName").description("The name of the plugin instance."))

  override protected def applicationDescription: String = "Handles plugin instances and requirements."

}
