package org.codeoverflow.chatoverflow.ui.web.rest.connector

import org.codeoverflow.chatoverflow.configuration.{Credentials, CryptoUtil}
import org.codeoverflow.chatoverflow.connector.ConnectorRegistry
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.{ConnectorDetails, ConnectorRef, CredentialsDetails, ResultMessage}
import org.scalatra.swagger.Swagger

import scala.collection.mutable

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

            val credentials = new Credentials(sourceIdentifier)
            if (!ConnectorRegistry.setConnectorCredentials(sourceIdentifier, uniqueTypeString, credentials)) {
              ResultMessage(success = false, "Unable to create credentials.")

            } else {
              chatOverflow.save()
              ResultMessage(success = true)
            }
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
      connector.get.removeCredentials()
      chatOverflow.save()
      ResultMessage(success = true)
    }
  }

  // TODO: Get one encrypted entry, post, delete

  get("/:sourceIdentifier/:qualifiedConnectorType/credentials", operation(getCredentials)) {
    if (!chatOverflow.isLoaded) {
      CredentialsDetails(found = false)
    } else {
      val connector = ConnectorRegistry.getConnector(params("sourceIdentifier"), params("qualifiedConnectorType"))

      if (connector.isEmpty) {
        CredentialsDetails(found = false)

      } else if (!connector.get.areCredentialsSet) {
        CredentialsDetails(found = false)

      } else {

        val requiredCredentials = getCredentialsMap(connector.get.getRequiredCredentialKeys, connector.get.getCredentials.get)
        val optionalCredentials = getCredentialsMap(connector.get.getOptionalCredentialKeys, connector.get.getCredentials.get)

        CredentialsDetails(found = true, requiredCredentials, optionalCredentials)
      }
    }
  }

  protected def getCredentialsMap(keys: List[String], credentials: Credentials): Map[String, String] = {
    val authKey = chatOverflow.credentialsService.generateAuthKey()
    val credentialsMap = mutable.Map[String, String]()

    for (key <- keys) {
      if (credentials.exists(key)) {
        val plainValue = credentials.getValue(key).get
        val encryptedValue = CryptoUtil.encryptSSLcompliant(authKey, plainValue)

        credentialsMap += key -> encryptedValue
      }
    }

    credentialsMap.toMap
  }

}
