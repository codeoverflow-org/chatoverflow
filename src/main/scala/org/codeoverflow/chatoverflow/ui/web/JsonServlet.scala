package org.codeoverflow.chatoverflow.ui.web

import org.codeoverflow.chatoverflow.{ChatOverflow, Launcher}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport

/**
  * A Json Servlet enables implicit json conversion for servlet output.
  */
abstract class JsonServlet extends ScalatraServlet with JacksonJsonSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats
  protected val chatOverflow: ChatOverflow = Launcher.server.get.chatOverflow

  before() {
    // Sets the return format to json only
    contentType = formats("json")

    // Allows cross origin requests
    options("/*") {
      response.setHeader(
        "Access-Control-Allow-Origin", "*")
    }
  }
}
