package org.codeoverflow.chatoverflow.ui.web.rest.connector

import org.codeoverflow.chatoverflow.configuration.{Credentials, CryptoUtil}
import org.codeoverflow.chatoverflow.connector.ConnectorRegistry
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs._
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
        if (!chatOverflow.isLoaded) {
          ResultMessage(success = false, "Framework not loaded.")

        } else {
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
  }

  delete("/:sourceIdentifier/:qualifiedConnectorType", operation(deleteConnector)) {
    val sourceIdentifier = params("sourceIdentifier")
    val qualifiedConnectorType = params("qualifiedConnectorType")

    val connector = ConnectorRegistry.getConnector(sourceIdentifier, qualifiedConnectorType)

    if (!chatOverflow.isLoaded) {
      ResultMessage(success = false, "Framework not loaded.")

    } else if (connector.isEmpty) {
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

  get("/:sourceIdentifier/:qualifiedConnectorType/credentials/:key", operation(getCredentialsEntry)) {
    val key = params("key")

    if (!chatOverflow.isLoaded) {
      CredentialsEntry(found = false)
    } else {
      val connector = ConnectorRegistry.getConnector(params("sourceIdentifier"), params("qualifiedConnectorType"))

      if (connector.isEmpty) {
        CredentialsEntry(found = false)

      } else if (!connector.get.areCredentialsSet) {
        CredentialsEntry(found = false)

      } else if (!connector.get.getCredentials.get.exists(key)) {
        CredentialsEntry(found = false)

      } else {
        val plainValue = connector.get.getCredentials.get.getValue(key).get
        val encryptedValue = encryptCredentialsValue(plainValue)

        CredentialsEntry(found = true, key, encryptedValue)
      }
    }
  }

  post("/:sourceIdentifier/:qualifiedConnectorType/credentials/", operation(postCredentialsEntry)) {
    parsedAs[EncryptedKeyValuePair] {
      case EncryptedKeyValuePair(key, value) =>
        if (!chatOverflow.isLoaded) {
          ResultMessage(success = false, "Chat Overflow is not loaded.")

        } else {
          val connector = ConnectorRegistry.getConnector(params("sourceIdentifier"), params("qualifiedConnectorType"))

          if (connector.isEmpty) {
            ResultMessage(success = false, "Connector not found.")

          } else if (!connector.get.areCredentialsSet) {
            ResultMessage(success = false, "No credentials subobject found.")

          } else if (connector.get.isRunning) {
            // Should hot swapping be a thing?
            ResultMessage(success = false, "Connector is running.")

          } else if (!connector.get.getRequiredCredentialKeys.contains(key) &&
            !connector.get.getOptionalCredentialKeys.contains(key)) {
            // Note that its only possible to add values to required and optional keys
            // Let's not dumb everything into the credentials object please
            ResultMessage(success = false, "Key is not required/optional for this connector.")

          } else {

            val decryptedValue = decryptCredentialsValue(value)

            if (decryptedValue.isEmpty) {
              ResultMessage(success = false, "Value encrypted with wrong auth key.")

            } else if (!connector.get.setCredentialsValue(key, decryptedValue.get)) {
              ResultMessage(success = false, "Unable to set credentials value.")

            } else {
              chatOverflow.save()
              ResultMessage(success = true)
            }
          }
        }
    }
  }

  delete("/:sourceIdentifier/:qualifiedConnectorType/credentials/:key", operation(deleteCredentialsEntry)) {
    parsedAs[AuthKey] {
      case AuthKey(authKey) =>
        val key = params("key")
        val frameworkAuthKey = chatOverflow.credentialsService.generateAuthKey()

        if (authKey != frameworkAuthKey) {
          ResultMessage(success = false, "Wrong auth key supplied.")
        } else {

          if (!chatOverflow.isLoaded) {
            ResultMessage(success = false, "Chat Overflow is not loaded.")
          } else {
            val connector = ConnectorRegistry.getConnector(params("sourceIdentifier"), params("qualifiedConnectorType"))

            if (connector.isEmpty) {
              ResultMessage(success = false, "Connector not found.")

            } else if (!connector.get.areCredentialsSet) {
              ResultMessage(success = false, "No credentials subobject found.")

            } else if (connector.get.isRunning) {
              ResultMessage(success = false, "Connector is running.")

            } else if (!connector.get.getCredentials.get.exists(key)) {
              ResultMessage(success = false, "Credentials key not found.")

            } else {
              connector.get.getCredentials.get.removeValue(key)
              chatOverflow.save()

              ResultMessage(success = true)
            }
          }
        }
    }
  }

  protected def getCredentialsMap(keys: List[String], credentials: Credentials): Map[String, String] = {
    val credentialsMap = mutable.Map[String, String]()

    for (key <- keys) {
      if (credentials.exists(key)) {
        val plainValue = credentials.getValue(key).get
        credentialsMap += key -> encryptCredentialsValue(plainValue)
      }
    }

    credentialsMap.toMap
  }

  protected def encryptCredentialsValue(plainValue: String): String = {
    val authKey = chatOverflow.credentialsService.generateAuthKey()
    CryptoUtil.encryptSSLcompliant(authKey, plainValue)
  }

  protected def decryptCredentialsValue(cipherValue: String): Option[String] = {
    val authKey = chatOverflow.credentialsService.generateAuthKey()
    CryptoUtil.decryptSSLcompliant(authKey, cipherValue)
  }

}
