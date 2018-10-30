package org.codeoverflow.chatoverflow.ui.web

import org.json4s.DefaultFormats
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.codeoverflow.chatoverflow.ChatOverflow
import org.codeoverflow.chatoverflow.configuration.Credentials
import org.scalatra.ScalatraServlet
import org.scalatra.scalate.ScalateSupport


/**
  * @author vetterd
  */
class ApiServlet extends ScalatraServlet with ScalateSupport {

  implicit val formats = DefaultFormats

  before() {
    contentType = "text/html"
  }

  post("/login") {
    params("password") match {
      case (password: String) => {
        ChatOverflow.loadCredentials(password)

        ChatOverflow.postInit()
        redirect("/dashboard")
      }
    }
  }

  post("/addConfiguration") {
    (params("newCredentialsIdentifier"), params("newCredentialsType"), params("newCredentialsKey"), params("newCredentialsValue")) match {
      case (identifier: String, credType: String, key: String, value: String) => {
        val credentials = new Credentials(identifier)
        credentials.addValue(key, value)
        ChatOverflow.credentialsService.addCredentials(credType, credentials)
        ChatOverflow.credentialsService.save()
        redirect("/dashboard")
      }
    }
  }

  post("/addPluginInstance") {
    (params("pluginInfo"), params("newPluginIdentifier")) match {
      case (pluginInfo: String, newPluginIdentifier: String) => {
        val pluginInfoJSON = jsonToMap(pluginInfo)
        val pluginName = pluginInfoJSON("name")
        val pluginAuthor = pluginInfoJSON("author")
        val pluginType = ChatOverflow.pluginFramework.getPluggable(pluginName, pluginAuthor).get
        ChatOverflow.pluginInstanceRegistry.addPluginInstance(newPluginIdentifier, pluginType)
      }
    }
    redirect("/dashboard")
  }

  get("/configurePluginInstance") {
    (params("instanceIdentifier")) match {
      case (instanceIdentifier: String) => {
        val requirements = ChatOverflow.pluginInstanceRegistry.getRequirements(instanceIdentifier)
        ssp(
          "/WEB-INF/pages/config/main.ssp",
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
