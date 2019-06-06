package org.codeoverflow.chatoverflow.ui.web.rest.connector

import org.codeoverflow.chatoverflow.connector.ConnectorKey
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs._
import org.codeoverflow.chatoverflow.ui.web.rest.{AuthSupport, TagSupport}
import org.scalatra.swagger.SwaggerSupport
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder

trait ConnectorControllerDefinition extends SwaggerSupport with TagSupport with AuthSupport {

  val getConnectors: OperationBuilder =
    (apiOperation[List[ConnectorKey]]("getConnectors")
      summary "Shows all connector keys."
      description "Shows the unique keys (connector type and source Identifier) of all connectors."
      parameter authHeader
      tags controllerTag)
  val getConnector: OperationBuilder =
    (apiOperation[ConnectorDetails]("getConnector")
      summary "Shows all information of a specific connector."
      description "Besides unique key, shows additional information like required and optional credentials. "
      tags controllerTag
      parameter authHeader
      parameter pathParam[String]("sourceIdentifier").description("The (connector unique) identifier of e.g. a account to connect to")
      parameter pathParam[String]("qualifiedConnectorType").description("The fully qualified type of the connector."))
  val postConnector: OperationBuilder =
    (apiOperation[ResultMessage]("postConnector")
      summary "Creates a new connector."
      description "Creates a connector with given sourceIdentifier and connector type."
      tags controllerTag
      parameter authHeader
      parameter bodyParam[ConnectorRef]("body").description("Requires platform specific source identifier and connector type."))
  val deleteConnector: OperationBuilder =
    (apiOperation[ResultMessage]("deleteConnector")
      summary "Deletes a specific connector."
      description "Deletes the connector specified by identifier and unique type string."
      tags controllerTag
      parameter authHeader
      parameter pathParam[String]("sourceIdentifier").description("The (connector unique) identifier of e.g. a account to connect to")
      parameter pathParam[String]("qualifiedConnectorType").description("The fully qualified type of the connector."))
  val getCredentials: OperationBuilder =
    (apiOperation[CredentialsDetails]("getCredentials")
      summary "Shows all credentials for a specified connector."
      description "Shows required and optional credentials. Note, that the user has to be logged in and the values are encrypted using the auth key. "
      tags controllerTag
      parameter authHeader
      parameter pathParam[String]("sourceIdentifier").description("The (connector unique) identifier of e.g. a account to connect to")
      parameter pathParam[String]("qualifiedConnectorType").description("The fully qualified type of the connector."))
  val getCredentialsEntry: OperationBuilder =
    (apiOperation[CredentialsEntry]("getCredentialsEntry")
      summary "Shows a specific credentials entry of a specific connector."
      description "Shows one credentials entry if existent. Note, that the user has to be logged in and the value is encrypted using the auth key. "
      tags controllerTag
      parameter authHeader
      parameter pathParam[String]("sourceIdentifier").description("The (connector unique) identifier of e.g. a account to connect to")
      parameter pathParam[String]("qualifiedConnectorType").description("The fully qualified type of the connector.")
      parameter pathParam[String]("key").description("The key of the credentials entry."))
  val postCredentialsEntry: OperationBuilder =
    (apiOperation[ResultMessage]("postCredentialsEntry")
      summary "Creates a new credentials entry."
      description "Creates a new credentials entry with given parameters for a given connector. Note that only required & optional keys can be added."
      tags controllerTag
      parameter authHeader
      parameter pathParam[String]("sourceIdentifier").description("The (connector unique) identifier of e.g. a account to connect to")
      parameter pathParam[String]("qualifiedConnectorType").description("The fully qualified type of the connector.")
      parameter bodyParam[EncryptedKeyValuePair]("body").description("Requires a key-value pair. The value must be encrypted using the auth key."))
  val deleteCredentialsEntry: OperationBuilder =
    (apiOperation[ResultMessage]("deleteCredentialsEntry")
      summary "Deletes a specific credentials entry."
      description "Deletes a specific credentials entry of a given connector, if possible."
      tags controllerTag
      parameter authHeader
      parameter pathParam[String]("sourceIdentifier").description("The (connector unique) identifier of e.g. a account to connect to")
      parameter pathParam[String]("qualifiedConnectorType").description("The fully qualified type of the connector.")
      parameter pathParam[String]("key").description("The key of the credentials entry."))

  override def controllerTag: String = "connector"

  override protected def applicationDescription: String = "Handles platform connectors."

}
