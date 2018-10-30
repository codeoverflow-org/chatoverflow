package org.codeoverflow.chatoverflow.ui.web

import org.codeoverflow.chatoverflow.ChatOverflow
import org.codeoverflow.chatoverflow.configuration.Credentials
import org.scalatra._
import org.scalatra.scalate.ScalateSupport

/**
  * @author vetterd
  */
class MainServlet extends ScalatraServlet with ScalateSupport {

  val defaultLayout = "/WEB-INF/layouts/default.ssp"

  before() {
    contentType = "text/html"
  }

  get("/dashboard") {
    val credentialTypesAndIdentifiers = ChatOverflow.credentialsService.getAllCredentials().map(_._1)
    val pluginInstances = ChatOverflow.pluginInstanceRegistry.getPluginInstances
    val availablePluginTypes = ChatOverflow.pluginFramework.getLoadedPlugins.sortBy(_.name)
    ssp(
      "/WEB-INF/pages/main/main.ssp",
      "layout" -> defaultLayout,
      "title" -> "CodeOverflow",
      "availablePluginTypes" -> availablePluginTypes,
      "configuredPlugins" -> pluginInstances,
      "credentials" -> credentialTypesAndIdentifiers
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
