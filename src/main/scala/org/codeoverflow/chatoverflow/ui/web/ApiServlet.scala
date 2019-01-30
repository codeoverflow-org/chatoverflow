package org.codeoverflow.chatoverflow.ui.web

import org.json4s.DefaultFormats
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.codeoverflow.chatoverflow.configuration.Credentials
import org.codeoverflow.chatoverflow.connector.ConnectorRegistry
import org.scalatra.ScalatraServlet
import org.scalatra.scalate.ScalateSupport


/**
  * @author vetterd
  */
class ApiServlet extends ScalatraServlet with ScalateSupport with WithChatOverflow {

  implicit val formats = DefaultFormats
  val defaultLayout = "/WEB-INF/layouts/default.ssp"

  before() {
    contentType = "text/html"
  }

  post("/login") {
    params("password") match {
      case (password: String) => {
        chatOverflow.credentialsService.setPassword(password.toCharArray)
        chatOverflow.load()
        redirect("/dashboard")
      }
    }
  }

  post("/addConfiguration") {
    (params("newCredentialsIdentifier"), params("newCredentialsType"), params("newCredentialsKey"), params("newCredentialsValue")) match {
      case (identifier: String, credType: String, key: String, value: String) => {
        val credentials = new Credentials(identifier)
        credentials.addValue(key, value)
        ConnectorRegistry.setConnectorCredentials(identifier, credType, credentials)
        redirect("/dashboard")
      }
    }
  }

  post("/addPluginInstance") {
    (params("pluginIndex"), params("newPluginIdentifier")) match {
      case (pluginIndex: String, newPluginIdentifier: String) => {
        val sortedPluginTypes = chatOverflow.pluginFramework.getPlugins.sortBy(pt => pt.getName + pt.getAuthor)
        val pluginType = sortedPluginTypes(pluginIndex.toInt)
        chatOverflow.pluginInstanceRegistry.addPluginInstance(newPluginIdentifier, pluginType)
      }
    }
    redirect("/dashboard")
  }

  get("/configurePluginInstance") {
    (params("instanceName")) match {
      case (instanceName: String) => {
        val requirements = chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName).get.getRequirements
        ssp(
          //TODO: Plugin config page (config needs to be filled before start)
          "/WEB-INF/pages/config/configurePlugin.ssp",
          "layout" -> defaultLayout,
          "requirements" -> requirements
        )
      }
    }
  }

  def jsonToMap(jsonString: String): Map[String, String] = {
    val cleanedJson = jsonString.replace('\'', '\"')
    parse(cleanedJson).extract[Map[String, String]]
  }
}
