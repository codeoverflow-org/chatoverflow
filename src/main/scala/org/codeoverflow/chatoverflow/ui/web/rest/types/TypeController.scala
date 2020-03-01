package org.codeoverflow.chatoverflow.ui.web.rest.types

import org.codeoverflow.chatoverflow.api.io.input.Input
import org.codeoverflow.chatoverflow.api.io.output.Output
import org.codeoverflow.chatoverflow.api.io.parameter.Parameter
import org.codeoverflow.chatoverflow.framework.{PluginMetadata => FrameworkPluginMetadata}
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs._
import org.scalatra.swagger.Swagger

import scala.collection.mutable.ListBuffer


class TypeController(implicit val swagger: Swagger) extends JsonServlet with TypesControllerDefinition {

  get("/plugin/", operation(getPluginType)) {
    authKeyRequired {
      getPluginTypes
    }
  }

  get("/requirement/", operation(getRequirementType)) {
    authKeyRequired {
      getRequirementTypes
    }
  }

  get("/connector/", operation(getConnectorType)) {
    authKeyRequired {
      getConnectorTypes
    }
  }

  get("/connector/metadata", operation(getConnectorsMetadata)) {
    authKeyRequired {
      getConnectorTypes.map(typeString => typeString -> readConnectorMetadata(typeString))
    }
  }

  get("/connector/metadata/:qualifiedConnectorType", operation(getConnectorMetadata)) {
    authKeyRequired {
      val qualifiedConnectorType = params("qualifiedConnectorType")
      readConnectorMetadata(qualifiedConnectorType)
    }
  }

  get("/", operation(getTypes)) {
    authKeyRequired {
      Types(getPluginTypes, getRequirementTypes, getConnectorTypes)
    }
  }

  get("/requirement/getRequirementImplementation", operation(getReqImpl)) {
    authKeyRequired {
      val apiType = params("api")
      val specificType = chatOverflow.typeRegistry.getRequirementImplementation(apiType)
      val connector = chatOverflow.typeRegistry.getRequirementConnectorLink(apiType)
      APIAndSpecificType(apiType, if (specificType.isDefined) specificType.get.getName else "",
        connector.getOrElse(""), specificType.isDefined)
    }
  }

  get("/requirement/getSubTypes", operation(getSubTypes)) {
    authKeyRequired {
      val apiType = params("api")
      SubTypes(apiType, chatOverflow.typeRegistry.getAllSubTypeInterfaces(apiType))
    }
  }

  private def getPluginTypes = chatOverflow.pluginFramework.getPlugins.map(pluginType => PluginType(
    pluginType.getName, pluginType.getAuthor, pluginType.getVersion, pluginType.getMajorAPIVersion,
    pluginType.getMinorAPIVersion, getPluginMetadata(pluginType.getMetadata), pluginType.getState.toString))

  private def getPluginMetadata(m: FrameworkPluginMetadata): PluginMetadata = PluginMetadata(m.description, m.license,
    m.website.map(_.toString), m.sourceRepo.map(_.toString), m.bugtracker.map(_.toString))

  private def getRequirementTypes = {
    var input = ListBuffer[String]()
    var output = ListBuffer[String]()
    var parameter = ListBuffer[String]()

    for ((typeString, cls) <- chatOverflow.typeRegistry.getRequirementTypes) {

      if (classOf[Input].isAssignableFrom(cls)) input += typeString
      if (classOf[Output].isAssignableFrom(cls)) output += typeString
      if (classOf[Parameter[_]].isAssignableFrom(cls)) parameter += typeString
    }

    RequirementTypes(input.toSeq, output.toSeq, parameter.toSeq)
  }

  private def getConnectorTypes = chatOverflow.typeRegistry.getConnectorTypes

  private def readConnectorMetadata(typeString: String): ConnectorMetadata = {

    val ressource = getClass.getResourceAsStream(s"/connector/${typeString.toLowerCase}.xml")

    if (ressource == null) {
      ConnectorMetadata(found = false, "", "", "", "")
    } else {
      val node = xml.Utility.trim(xml.XML.load(ressource))
      ConnectorMetadata(found = true,
        (node \ "display").text,
        (node \ "description").text,
        (node \ "wiki").text,
        (node \ "icon48").text)
    }
  }

}
