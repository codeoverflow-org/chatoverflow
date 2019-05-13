package org.codeoverflow.chatoverflow.ui.web

import org.codeoverflow.chatoverflow.{ChatOverflow, Launcher}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.{CorsSupport, ScalatraServlet}

/**
  * A Json Servlet enables implicit json conversion for servlet output.
  */
abstract class JsonServlet extends ScalatraServlet with JacksonJsonSupport with CorsSupport {
  protected implicit val jsonFormats: Formats = DefaultFormats
  protected val chatOverflow: ChatOverflow = Launcher.server.get.chatOverflow

  // Sets the return format to json only
  before() {
    contentType = formats("json")
  }
}
