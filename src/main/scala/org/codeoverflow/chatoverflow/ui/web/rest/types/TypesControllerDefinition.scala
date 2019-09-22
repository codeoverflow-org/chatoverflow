package org.codeoverflow.chatoverflow.ui.web.rest.types

import org.codeoverflow.chatoverflow.ui.web.rest.DTOs._
import org.codeoverflow.chatoverflow.ui.web.rest.{AuthSupport, TagSupport}
import org.scalatra.swagger.SwaggerSupport
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder

trait TypesControllerDefinition extends SwaggerSupport with TagSupport with AuthSupport {

  val getPluginType: OperationBuilder =
    (apiOperation[List[PluginType]]("getPlugin")
      summary "Shows all possible plugin types."
      description "Shows name, author, api version and state of all possible plugin types."
      parameter authHeader
      tags controllerTag)
  val getRequirementType: OperationBuilder =
    (apiOperation[RequirementTypes]("getRequirementType")
      summary "Shows all possible requirement types."
      description "Shows the fully qualified api type strings of all requirement types, grouped by input/output/parameter."
      parameter authHeader
      tags controllerTag)
  val getConnectorType: OperationBuilder =
    (apiOperation[List[String]]("getConnectorType")
      summary "Shows all possible connector types."
      description "Shows the fully qualified type strings of all connectors."
      parameter authHeader
      tags controllerTag)
  val getConnectorsMetadata: OperationBuilder =
    (apiOperation[Map[String, ConnectorMetadata]]("getConnectorsMetadata")
      summary "Shows the types of all connectors. If available, also the metadata is showed."
      description "Shows a map of connector type string and metadata with display name, description, wiki url and base64 encoded icon."
      parameter authHeader
      tags controllerTag)
  val getConnectorMetadata: OperationBuilder =
    (apiOperation[ConnectorMetadata]("getConnectorMetadata")
      summary "Shows the metadata of a specified connector, if found."
      description "Shows the connectors metadata with display name, description, wiki url and base64 encoded icon."
      parameter pathParam[String]("qualifiedConnectorType").description("The qualified connector type string of a registered connector.")
      parameter authHeader
      tags controllerTag)
  val getTypes: OperationBuilder =
    (apiOperation[Types]("getTypes")
      summary "Shows all possible types of plugins, requirements and connectors."
      description "Shows all possible types of plugins, requirements and connectors."
      parameter authHeader
      tags controllerTag)
  val getReqImpl: OperationBuilder =
    (apiOperation[APIAndSpecificType]("getReqImpl")
      summary "Shows all implementations of a specified api type."
      description "Shows the specific type (implementation) and the connector of a specified api type."
      parameter authHeader
      tags controllerTag
      parameter queryParam[String]("api").description("The fully qualified api type string"))
  val getSubTypes: OperationBuilder =
    (apiOperation[SubTypes]("getSubTypes")
      summary "Shows all sub types of a specified api type."
      description "Shows all sub types (sub typing api interfaces) of a specified api type."
      parameter authHeader
      tags controllerTag
      parameter queryParam[String]("api").description("The fully qualified api type string"))

  override def controllerTag: String = "type"

  override protected def applicationDescription: String = "Handles requirement, plugin and controller types."

}
