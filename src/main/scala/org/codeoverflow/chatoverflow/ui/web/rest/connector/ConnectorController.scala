package org.codeoverflow.chatoverflow.ui.web.rest.connector

import org.codeoverflow.chatoverflow.connector.ConnectorRegistry
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.ConnectorDetails
import org.scalatra.swagger.Swagger

class ConnectorController(implicit val swagger: Swagger) extends JsonServlet with ConnectorControllerDefinition {

  get("/", operation(getConnectors)) {
    ConnectorRegistry.getConnectorKeys
  }

  get("/:sourceIdentifier/:qualifiedConnectorType", operation(getConnector)) {
    val connector = ConnectorRegistry.getConnector(params("sourceIdentifier"), params("qualifiedConnectorType"))
    if (connector.isEmpty) {
      ConnectorDetails(found = false, "", "", areCredentialsSet = false, isRunning = false, Seq[String](), Seq[String]())
    } else {
      ConnectorDetails(found = true, connector.get.sourceIdentifier, connector.get.getUniqueTypeString,
        areCredentialsSet = connector.get.areCredentialsSet, isRunning = connector.get.isRunning,
        connector.get.getRequiredCredentialKeys, connector.get.getOptionalCredentialKeys)
    }
  }

}
