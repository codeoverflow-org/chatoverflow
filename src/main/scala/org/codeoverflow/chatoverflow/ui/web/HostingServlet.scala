package org.codeoverflow.chatoverflow.ui.web

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.ConnectorRegistry
import org.scalatra.ScalatraServlet


class HostingServlet extends ScalatraServlet with WithLogger {

  get(s"/:connectorScope/:pluginScope") {
    val connectorScope = params("connectorScope")
    val pluginScope = params("pluginScope")

    val connectorKey = ConnectorRegistry.getConnector(connectorScope, HostingServlet.hostingConnectorQualifiedConnectorName)

    s"connector = $connectorScope, plugin= $pluginScope, connectorFound = ${connectorKey.isDefined}"
  }

}

object HostingServlet {
  val hostingConnectorQualifiedConnectorName = "org.codeoverflow.chatoverflow.requirement.hosting.HostingConnector"
}