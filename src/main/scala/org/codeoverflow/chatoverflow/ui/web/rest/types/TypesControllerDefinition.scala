package org.codeoverflow.chatoverflow.ui.web.rest.types

import org.codeoverflow.chatoverflow.ui.web.rest.DTOs._
import org.scalatra.swagger.SwaggerSupport
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder

trait TypesControllerDefinition extends SwaggerSupport {

  val getPluginType: OperationBuilder =
    (apiOperation[List[PluginType]]("getPlugin")
      summary "Shows all possible plugin types."
      description "Shows name, author, api version and state of all possible plugin types.")

  val getRequirementType: OperationBuilder =
    (apiOperation[RequirementTypes]("getRequirementType")
      summary "Shows all possible requirement types."
      description "Shows the fully qualified api type strings of all requirement types, grouped by input/output/parameter.")

  val getConnectorType: OperationBuilder =
    (apiOperation[List[String]]("getConnectorType")
      summary "Shows all possible connector types."
      description "Shows the fully qualified type strings of all connectors.")

  val getTypes: OperationBuilder =
    (apiOperation[Types]("getTypes")
      summary "Shows all possible types of plugins, requirements and connectors."
      description "Shows all possible types of plugins, requirements and connectors.")

  val getReqImpl: OperationBuilder =
    (apiOperation[APIAndSpecificType]("getReqImpl")
      summary "Shows all implementations of a specified api type."
      description "Shows the specific type (implementation) and the connector of a specified api type."
      parameter queryParam[String]("api").description("The fully qualified api type string"))

  val getSubTypes: OperationBuilder =
    (apiOperation[SubTypes]("getSubTypes")
      summary "Shows all sub types of a specified api type."
      description "Shows all sub types (sub typing api interfaces) of a specified api type."
      parameter queryParam[String]("api").description("The fully qualified api type string"))


  override protected def applicationDescription: String = "Handles requirement, plugin and controller types."

}
