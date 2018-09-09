package org.codeoverflow.chatoverflow.ui.web

import org.codeoverflow.chatoverflow.ChatOverflow
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
        redirect("/main")
      }
    }
  }

}
