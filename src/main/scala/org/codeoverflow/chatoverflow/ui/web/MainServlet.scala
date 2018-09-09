package org.codeoverflow.chatoverflow.ui.web

import org.scalatra._
import org.scalatra.scalate.ScalateSupport

/**
  * @author vetterd
  */
class CodeOverflowServlet extends ScalatraServlet with ScalateSupport {

  val defaultLayout = "/WEB-INF/layouts/default.ssp"

  before() {
    contentType = "text/html"
  }

  get("/main") {
    ssp(
      "/WEB-INF/pages/main.ssp",
      "layout" -> defaultLayout,
      "title" -> "CodeOverflow"
    )
  }

  get("/") {
    ssp(
      "/WEB-INF/pages/login.ssp",
      "layout" -> defaultLayout,
      "title" -> "CodeOverflow Login Page"
    )
  }
}
