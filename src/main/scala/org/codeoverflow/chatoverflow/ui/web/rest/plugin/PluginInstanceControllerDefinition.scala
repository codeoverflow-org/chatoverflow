package org.codeoverflow.chatoverflow.ui.web.rest.plugin

import org.codeoverflow.chatoverflow.ui.web.rest.DTOs._
import org.codeoverflow.chatoverflow.ui.web.rest.{AuthSupport, TagSupport}
import org.scalatra.swagger.SwaggerSupport
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder

trait PluginInstanceControllerDefinition extends SwaggerSupport with TagSupport with AuthSupport {

  val getInstances: OperationBuilder =
    (apiOperation[List[PluginInstance]]("getInstances")
      summary "Shows all plugin instances."
      description "Shows the name, plugin information and requirement keys of all plugin instances."
      parameter authHeader
      tags controllerTag)
  val getInstance: OperationBuilder =
    (apiOperation[PluginInstance]("getInstance")
      summary "Shows a plugin instance."
      description "Shows the name, plugin information and requirement keys of a specified plugin instance."
      tags controllerTag
      parameter authHeader
      parameter pathParam[String]("instanceName").description("The name of the plugin instance."))
  val getRequirements: OperationBuilder =
    (apiOperation[List[Requirement]]("getRequirements")
      summary "Shows all requirements of a plugin instance."
      description "Shows state, name and type of all requirements of a specified plugin instance."
      tags controllerTag
      parameter authHeader
      parameter pathParam[String]("instanceName").description("The name of the plugin instance."))
  val getRequirement: OperationBuilder =
    (apiOperation[Requirement]("getRequirement")
      summary "Shows a specific requirement of a plugin instance."
      description "Shows state, name and type of one specific requirement of a specified plugin instance."
      tags controllerTag
      parameter authHeader
      parameter pathParam[String]("instanceName").description("The name of the plugin instance.")
      parameter pathParam[String]("requirementID").description("The unique id of the requirement."))
  val putRequirement: OperationBuilder =
    (apiOperation[ResultMessage]("putRequirement")
      summary "Changes the value and type of a specific requirement."
      description "Changes the value (serialized content) and the target type of a requirement of one instance."
      tags controllerTag
      parameter authHeader
      parameter pathParam[String]("instanceName").description("The name of the plugin instance.")
      parameter pathParam[String]("requirementID").description("The unique id of the requirement.")
      parameter bodyParam[RequirementInfo]("body").description("Requires target type and serialized content."))
  val deleteRequirement: OperationBuilder =
    (apiOperation[ResultMessage]("deleteRequirement")
      summary "Removes a specific requirement."
      description "Removes a specific requirement by unseting its value."
      tags controllerTag
      parameter authHeader
      parameter pathParam[String]("instanceName").description("The name of the plugin instance.")
      parameter pathParam[String]("requirementID").description("The unique id of the requirement."))
  val getLog: OperationBuilder =
    (apiOperation[List[String]]("getLog")
      summary "Shows the log of a plugin instance."
      description "Shows all (or the newest) log messages of a specified plugin instance."
      tags controllerTag
      parameter authHeader
      parameter pathParam[String]("instanceName").description("The name of the plugin instance.")
      parameter queryParam[Option[String]]("startIndex").description("The start index of the message stream").optional)
  val postInstance: OperationBuilder =
    (apiOperation[ResultMessage]("postInstance")
      summary "Creates a new plugin instance."
      description "Creates a new plugin instance with given name and plugin type."
      tags controllerTag
      parameter authHeader
      parameter bodyParam[PluginInstanceRef]("body").description("Requires new instance name and PluginType (name and author)."))
  val deleteInstance: OperationBuilder =
    (apiOperation[ResultMessage]("deleteInstance")
      summary "Removes a plugin instance."
      description "Removes a plugin instance specified by its name, if possible."
      tags controllerTag
      parameter authHeader
      parameter pathParam[String]("instanceName").description("The name of the plugin instance."))
  val startInstance: OperationBuilder =
    (apiOperation[ResultMessage]("startInstance")
      summary "Starts a specific plugin instance."
      description "Starts a specific plugin instance if possible."
      tags controllerTag
      parameter authHeader
      parameter bodyParam[PluginInstanceName]("body").description("Requires the name of the plugin instance."))
  val stopInstance: OperationBuilder =
    (apiOperation[ResultMessage]("stopInstance")
      summary "Stops a specific plugin instance."
      description "Requires the stop of a specified plugin instance. This can take up to one loop interval."
      tags controllerTag
      parameter authHeader
      parameter bodyParam[PluginInstanceName]("body").description("Requires the name of the plugin instance."))

  override def controllerTag: String = "instance"

  override protected def applicationDescription: String = "Handles plugin instances and requirements."

}
