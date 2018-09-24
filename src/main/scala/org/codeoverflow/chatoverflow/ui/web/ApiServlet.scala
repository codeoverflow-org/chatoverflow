package org.codeoverflow.chatoverflow.ui.web

import org.codeoverflow.chatoverflow.ChatOverflow
import org.codeoverflow.chatoverflow.configuration.Credentials
import org.scalatra.ScalatraServlet
import org.scalatra.scalate.ScalateSupport

/**
  * @author vetterd
  */
class ApiServlet extends ScalatraServlet with ScalateSupport {

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

}
