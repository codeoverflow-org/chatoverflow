package org.codeoverflow.chatoverflow.registry


import java.lang.reflect.Constructor

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.configuration.Credentials
import org.codeoverflow.chatoverflow.service.Connector

import scala.collection.mutable

object ConnectorRegistry {
  private val sources = mutable.Map[SourceKey, Connector]()
  private val logger = Logger.getLogger(this.getClass)

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

  def getConnector(connectorType: String, sourceIdentifier: String): Option[Connector] = {
    val key = SourceKey(connectorType, sourceIdentifier)
    sources.get(key)
  }

}

case class SourceKey(connectorType: String, sourceIdentifier: String)

object SourceKey {
  def apply(connector: Connector): SourceKey = SourceKey(connector.getUniqueTypeString, connector.sourceIdentifier)
}
