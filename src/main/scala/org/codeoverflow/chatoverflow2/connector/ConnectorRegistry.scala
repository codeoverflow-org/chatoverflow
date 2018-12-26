package org.codeoverflow.chatoverflow2.connector

import java.lang.reflect.Constructor

import org.codeoverflow.chatoverflow2.WithLogger
import org.codeoverflow.chatoverflow2.configuration.Credentials
import org.codeoverflow.chatoverflow2.registry.TypeRegistry

import scala.collection.mutable

object ConnectorRegistry extends WithLogger {
  private val connectors = mutable.Map[ConnectorKey, Connector]()
  private var typeRegistry: Option[TypeRegistry] = None

  def setTypeRegistry(typeRegistry: TypeRegistry): Unit = this.typeRegistry = Some(typeRegistry)

  def addConnector(sourceIdentifier: String, qualifiedConnectorName: String): Boolean = {
    logger info s"Trying to add connector '$sourceIdentifier' of type '$qualifiedConnectorName'."
    val connectorKey = ConnectorKey(sourceIdentifier, qualifiedConnectorName)

    // Impossible to add new connectors without a proper instantiated type registry
    if (typeRegistry.isEmpty) {
      logger error "No proper type registry found. Unable to add any connectors."
      false
    } else {

      // Check if the connector was already added
      if (connectors.contains(connectorKey)) {
        logger warn "Unable to add connector. A connector with the given type and name does already exist."
        false
      } else {

        // Retrieve class from TypeRegistry
        val connectorClass = typeRegistry.get.getConnectorType(qualifiedConnectorName)
        if (connectorClass.isEmpty) {
          logger warn "Unable to find connector class in the type registry. Unable to add connector."
          false

          // Instantiate everything correctly
        } else {
          logger info "Found connector class."
          try {
            val connectorConstructor: Constructor[_] = connectorClass.get.getConstructor(classOf[String])
            val connector: Connector = connectorConstructor.
              newInstance(sourceIdentifier).asInstanceOf[Connector]

            connectors += connectorKey -> connector
            logger info "Successfully created and registered connector."
            true
          } catch {
            case _: ClassNotFoundException =>
              logger warn "Unable to find connector class."
              false
            case _: NoSuchMethodException =>
              logger warn "Unable to find correct constructor."
              false
            case e: Exception =>
              logger warn s"Unable to create connector. A wild exception appeared: ${e.getMessage}"
              false
          }
        }
      }
    }
  }

  def setConnectorCredentials(sourceIdentifier: String, qualifiedConnectorName: String, credentials: Credentials): Boolean = {
    logger info s"Trying to add credentials for connector '$sourceIdentifier' of type '$qualifiedConnectorName'."
    val connector = getConnector(sourceIdentifier, qualifiedConnectorName)

    // Check if connector was already defined
    if (connector.isEmpty) {
      logger warn "Unable to find connector. Unable to add credentials."
      false

    } else {

      // Set credentials
      connector.get.setCredentials(credentials)
      logger info "Successfully added credentials to connector."

      // Check if all required keys had been set
      if (!connector.get.getRequiredCredentialKeys.forall(key => credentials.exists(key))) {
        logger warn "Not all required values had been set. But they might be optional."
      }

      true
    }
  }


  def getConnector(sourceIdentifier: String, qualifiedConnectorName: String): Option[Connector] = {
    val connectorKey = ConnectorKey(sourceIdentifier, qualifiedConnectorName)
    connectors.get(connectorKey)
  }
}
