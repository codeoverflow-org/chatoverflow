package org.codeoverflow.chatoverflow.ui.web.rest.connector

import org.codeoverflow.chatoverflow.connector.ConnectorRegistry
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.{ConnectorDetails, ConnectorRef, ResultMessage}
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

  post("/", operation(postConnector)) {
    parsedAs[ConnectorRef] {
      case ConnectorRef(sourceIdentifier, uniqueTypeString) =>
        val connector = ConnectorRegistry.getConnector(sourceIdentifier, uniqueTypeString)

        if (connector.isDefined) {
          ResultMessage(success = false, "Connector already defined.")
        } else {
          val connectorClass = chatOverflow.typeRegistry.getConnectorType(uniqueTypeString)

          if (connectorClass.isEmpty) {
            ResultMessage(success = false, "Connector type not found.")

          } else if (!ConnectorRegistry.addConnector(sourceIdentifier, uniqueTypeString)) {
            ResultMessage(success = false, "Unable to add connector.")

          } else {
            chatOverflow.save()
            ResultMessage(success = true)
          }
        }
    }
  }

  delete("/:sourceIdentifier/:qualifiedConnectorType", operation(deleteConnector)) {
    val sourceIdentifier = params("sourceIdentifier")
    val qualifiedConnectorType = params("qualifiedConnectorType")

    val connector = ConnectorRegistry.getConnector(sourceIdentifier, qualifiedConnectorType)

    if (connector.isEmpty) {
      ResultMessage(success = false, "Connector does not exist.")

    } else if (connector.get.isRunning) {
      ResultMessage(success = false, "Connector is running.")

    } else if (!ConnectorRegistry.removeConnector(sourceIdentifier, qualifiedConnectorType)) {
      ResultMessage(success = false, "Unable to remove connector.")

    } else {
      chatOverflow.save()
      ResultMessage(success = true)
    }
  }

}
