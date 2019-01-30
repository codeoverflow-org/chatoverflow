package org.codeoverflow.chatoverflow.ui.web

import org.codeoverflow.chatoverflow.ChatOverflow
import org.codeoverflow.chatoverflow.configuration.Credentials
import org.codeoverflow.chatoverflow.connector.ConnectorRegistry
import org.scalatra._
import org.scalatra.scalate.ScalateSupport

/**
  * @author vetterd
  */
class MainServlet extends ScalatraServlet with ScalateSupport with WithChatOverflow {

  val defaultLayout = "/WEB-INF/layouts/default.ssp"

  before() {
    contentType = "text/html"
  }

  get("/dashboard") {
    val availableConnectors = ConnectorRegistry.getConnectorKeys.map(key => (key.qualifiedConnectorName, key.sourceIdentifier))
    val pluginInstances = chatOverflow.pluginInstanceRegistry.getAllPluginInstances
    val sortedPluginTypes = chatOverflow.pluginFramework.getPlugins.sortBy(pt => pt.getName+pt.getAuthor)
    val pluginTypesWithIndex = sortedPluginTypes.zipWithIndex
    ssp(
      "/WEB-INF/pages/main/main.ssp",
      "layout" -> defaultLayout,
      "title" -> "CodeOverflow",
      "pluginTypesWithIndex" -> pluginTypesWithIndex,
      "configuredPlugins" -> pluginInstances,
      "availableConnectors" -> availableConnectors
    )
  }

  get("/") {
    ssp(
      "/WEB-INF/pages/login/login.ssp",
      "layout" -> defaultLayout,
      "title" -> "CodeOverflow Login Page"
    )
  }
}
