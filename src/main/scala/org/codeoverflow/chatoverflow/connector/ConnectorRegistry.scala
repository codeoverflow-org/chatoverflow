package org.codeoverflow.chatoverflow.connector

import java.lang.reflect.Constructor

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.configuration.Credentials
import org.codeoverflow.chatoverflow.registry.TypeRegistry

import scala.collection.mutable

/**
  * The connector registry is used to manage connector instances.
  */
object ConnectorRegistry extends WithLogger {
  private val connectors = mutable.Map[ConnectorKey, Connector]()
  private var typeRegistry: Option[TypeRegistry] = None

  /**
    * Sets the type registry which is used while adding a new connector instance.
    *
    * @param typeRegistry the type registry object used in the chat overflow module
    */
  def setTypeRegistry(typeRegistry: TypeRegistry): Unit = this.typeRegistry = Some(typeRegistry)

  /**
    * Adds a new connector to the registry. Before instantiating the state is checked to be correct.
    *
    * @param sourceIdentifier       the identifier for the source platform
    * @param qualifiedConnectorName a fully qualified connector type string
    * @return false, if a major error happened
    */
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

  /**
    * Sets the credentials for a specified connector.
    *
    * @param sourceIdentifier       the identifier of the connector to retrieve it from the registry
    * @param qualifiedConnectorName the full qualified connector type string
    * @param credentials            the credentials object to set for the specified connector
    * @return false, if a major error happened
    */
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

  /**
    * Returns the connector specified by identifier and type
    *
    * @param sourceIdentifier       the identifier for the connector source
    * @param qualifiedConnectorName the full qualified connector type string
    * @return an optional holding the connector or none
    */
  def getConnector(sourceIdentifier: String, qualifiedConnectorName: String): Option[Connector] = {
    val connectorKey = ConnectorKey(sourceIdentifier, qualifiedConnectorName)
    connectors.get(connectorKey)
  }

  /**
    * Returns a list of connector keys for all registered connector instances.
    *
    * @return a list of connector key object containing connector type strings and identifiers
    */
  def getConnectorKeys: List[ConnectorKey] = connectors.keys.toList
}
