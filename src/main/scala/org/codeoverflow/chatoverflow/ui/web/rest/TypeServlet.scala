package org.codeoverflow.chatoverflow.ui.web.rest

import org.codeoverflow.chatoverflow.api.io.input.Input
import org.codeoverflow.chatoverflow.api.io.output.Output
import org.codeoverflow.chatoverflow.api.io.parameter.Parameter
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.{PluginType, RequirementTypes, Types}

import scala.collection.mutable.ListBuffer

class TypeServlet extends JsonServlet {

  get("/plugin/") {
    getPluginTypes
  }

  get("/requirement/") {
    getRequirementTypes
  }

  get("/connector/") {
    getConnectorTypes
  }

  get("/") {
    Types(getPluginTypes, getRequirementTypes, getConnectorTypes)
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
