package org.codeoverflow.chatoverflow.ui.web

import javax.servlet.http.HttpServletRequest
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.ResultMessage
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

  /**
    * Utility function to parse incoming json-body-arguments. Uses a lot of scala magic. Magical!
    *
    * @param func    the function (taking an object T, returning a ResultMessage) to call, if the parsing process was successful
    * @param request the request, implicitly provided by the scalatra environment
    * @tparam T the type of body you want (probably a case class)
    * @return a ResultMessage-object containing only true if successful, otherwise false and a error message
    */
  protected def parsedAs[T: Manifest](func: T => ResultMessage)(implicit request: HttpServletRequest): ResultMessage = {
    val errorMessage = ResultMessage(success = false, "Unable to parse. Wrong body-format.")

    if (request.body == "") {
      // No body provided
      errorMessage
    } else {
      // Parse json, extract case class, execute func
      val json = parse(request.body)
      val data = json.extractOpt[T](DefaultFormats, manifest[T])

      if (data.isEmpty) {
        errorMessage
      } else {
        func(data.get)
      }
    }
  }
}
