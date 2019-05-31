package org.codeoverflow.chatoverflow.ui.web.rest.types

import org.codeoverflow.chatoverflow.api.io.input.Input
import org.codeoverflow.chatoverflow.api.io.output.Output
import org.codeoverflow.chatoverflow.api.io.parameter.Parameter
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs._
import org.scalatra.swagger.Swagger

import scala.collection.mutable.ListBuffer

class TypeController(implicit val swagger: Swagger) extends JsonServlet with TypesControllerDefinition {

  get("/plugin/", operation(getPluginType)) {
    getPluginTypes
  }

  get("/requirement/", operation(getRequirementType)) {
    getRequirementTypes
  }

  get("/connector/", operation(getConnectorType)) {
    getConnectorTypes
  }

  get("/", operation(getTypes)) {
    Types(getPluginTypes, getRequirementTypes, getConnectorTypes)
  }

  get("/requirement/getRequirementImplementation", operation(getReqImpl)) {
    val apiType = params("api")
    val specificType = chatOverflow.typeRegistry.getRequirementImplementation(apiType)
    val connector = chatOverflow.typeRegistry.getRequirementConnectorLink(apiType)
    APIAndSpecificType(apiType, if (specificType.isDefined) specificType.get.getName else "",
      connector.getOrElse(""), specificType.isDefined)
  }

  get("/requirement/getSubTypes", operation(getSubTypes)) {
    val apiType = params("api")
    SubTypes(apiType, chatOverflow.typeRegistry.getAllSubTypeInterfaces(apiType))
  }

  private def getPluginTypes = chatOverflow.pluginFramework.getPlugins.map(pluginType => PluginType(pluginType.getName, pluginType.getAuthor,
    pluginType.getDescription, pluginType.getMajorAPIVersion, pluginType.getMinorAPIVersion, pluginType.getState.toString))

  private def getRequirementTypes = {
    var input = ListBuffer[String]()
    var output = ListBuffer[String]()
    var parameter = ListBuffer[String]()

    for ((typeString, cls) <- chatOverflow.typeRegistry.getRequirementTypes) {

      if (classOf[Input].isAssignableFrom(cls)) input += typeString
      if (classOf[Output].isAssignableFrom(cls)) output += typeString
      if (classOf[Parameter[_]].isAssignableFrom(cls)) parameter += typeString
    }

    RequirementTypes(input, output, parameter)
  }

  private def getConnectorTypes = chatOverflow.typeRegistry.getConnectorTypes

}
