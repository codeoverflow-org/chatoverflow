package org.codeoverflow.chatoverflow.ui.web.rest

import org.codeoverflow.chatoverflow.connector.ConnectorRegistry
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.ConnectorDetails

class ConnectorServlet extends JsonServlet {

  get("/") {
    ConnectorRegistry.getConnectorKeys
  }

  get("/:sourceIdentifier/:qualifiedConnectorType") {
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
