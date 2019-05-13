package org.codeoverflow.chatoverflow.ui.web.rest.connector

import org.codeoverflow.chatoverflow.connector.ConnectorKey
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.ConnectorDetails
import org.scalatra.swagger.SwaggerSupport
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder

trait ConnectorControllerDefinition extends SwaggerSupport {

  val getConnectors: OperationBuilder =
    (apiOperation[ConnectorKey]("getConnectors")
      summary "Shows all connector keys."
      description "Shows the unique keys (connector type and source Identifier) of all connectors.")

  val getConnector: OperationBuilder =
    (apiOperation[ConnectorDetails]("getConnector")
      summary "Shows all information of a specific connector."
      description "Besides unique key, shows additional information like required and optional credentials. "
      parameter pathParam[String]("sourceIdentifier").description("The (connector unique) identifier of e.g. a account to connect to")
      parameter pathParam[String]("qualifiedConnectorType").description("The fully qualified type of the connector."))

  override protected def applicationDescription: String = "Handles platform connectors."


}
