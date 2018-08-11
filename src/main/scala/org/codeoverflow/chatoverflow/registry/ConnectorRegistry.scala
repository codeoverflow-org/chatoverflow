package org.codeoverflow.chatoverflow.registry


import java.lang.reflect.Constructor

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.configuration.Credentials
import org.codeoverflow.chatoverflow.service.Connector

import scala.collection.mutable

/**
  * The connector registry holds all loaded connectors, ready to connect to the source (or already connected).
  */
object ConnectorRegistry {
  private val sources = mutable.Map[SourceKey, Connector]()
  private val logger = Logger.getLogger(this.getClass)

  /**
    * Adds a new connector to the registry. The connector is instantiated dynamically.
    *
    * @param connectorType    the connector type, which will be instantiated using reflection
    * @param sourceIdentifier the source identifier, e.g. the login name for a platform to connect to
    * @param credentials      the login data, needed to connect the connector to his platform
    */
  def addConnector(connectorType: String, sourceIdentifier: String, credentials: Option[Credentials]): Unit = {
    if (credentials.isEmpty) {
      logger warn s"Unable to find credentials."
    } else {
      logger info "Found credentials. Trying to create connector now."

      try {
        val connectorClass: Class[_] = Class.forName(connectorType)
        logger info "Found connector class."

        val connectorConstructor: Constructor[_] = connectorClass.getConstructor(classOf[String], classOf[Credentials])
        val connector: Connector = connectorConstructor.
          newInstance(sourceIdentifier, credentials.get).asInstanceOf[Connector]

        val key = SourceKey(connector)
        if (!sources.contains(key)) {
          sources += key -> connector
        } else {
          throw new IllegalArgumentException(s"Source key '$key' does already exist!")
        }

        logger info "Successfully created and registered connector."
      } catch {
        case _: ClassNotFoundException => logger warn "Unable to find connector class."
        case _: NoSuchMethodException => logger warn "Unable to find correct constructor."
        case e: Exception => logger warn s"Unable to create connector. A wild exception appeared: ${e.getMessage}"
      }
    }
  }

  /**
    * Returns a connector of the specified type for the given source, if it was registered before.
    *
    * @param connectorType    the dynamic type of the connector
    * @param sourceIdentifier the source identifier, e.g. a login name
    * @return an option, filled with the connector, if found
    */
  def getConnector(connectorType: String, sourceIdentifier: String): Option[Connector] = {
    val key = SourceKey(connectorType, sourceIdentifier)
    sources.get(key)
  }

}

/**
  * The source key of a connector is a combination of its type and its source identifier. This pair should be unique.
  *
  * @param connectorType    the dynamic type of the connector
  * @param sourceIdentifier the source identifier, e.g. a login name
  */
case class SourceKey(connectorType: String, sourceIdentifier: String)

object SourceKey {
  def apply(connector: Connector): SourceKey = SourceKey(connector.getUniqueTypeString, connector.sourceIdentifier)
}
