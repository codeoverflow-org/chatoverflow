package org.codeoverflow.chatoverflow.ui.web

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.ConnectorRegistry
import org.codeoverflow.chatoverflow.requirement.service.hosting.HostingConnector
import org.scalatra.ScalatraServlet


class HostingServlet extends ScalatraServlet with WithLogger {

  get(s"/:connectorScope/:pluginScope") {
    val connectorScope = params("connectorScope")
    val pluginScope = params("pluginScope")

    val connector = ConnectorRegistry.getConnector(connectorScope, HostingServlet.hostingConnectorQualifiedConnectorName)

    s"connector = $connectorScope, plugin= $pluginScope, connectorFound = ${connector.isDefined}"
  }

}

object HostingServlet {
  val hostingConnectorQualifiedConnectorName: String = classOf[HostingConnector].getTypeName
}