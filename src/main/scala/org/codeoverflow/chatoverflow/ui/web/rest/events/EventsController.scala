package org.codeoverflow.chatoverflow.ui.web.rest.events

import java.io.PrintWriter
import java.util.concurrent.ConcurrentHashMap

import javax.servlet.AsyncContext
import javax.servlet.http.HttpServletRequest
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.scalatra.servlet.ScalatraAsyncSupport
import org.scalatra.{BadRequest, Unauthorized}
import org.scalatra.swagger.Swagger

class EventsController(implicit val swagger: Swagger) extends JsonServlet with ScalatraAsyncSupport with EventsControllerDefinition {
  private val connectionWriters = new ConcurrentHashMap[AsyncContext, PrintWriter]()

  def broadcast(messageType: String, message: String = null): Unit = {
    connectionWriters.forEach((_, writer) => {
      try {
        sendMessage(writer, messageType, message)
      } catch {
        //probably lost or closed connection, remove from the list of connected clients
        case _: Throwable => connectionWriters.remove(writer)
      }
    })
  }

  def closeConnections(): Unit = {
    connectionWriters.forEach((_, writer) => {
      try {
        sendMessage(writer, "close", null)
        writer.close()
      } finally {
        connectionWriters.remove(writer)
      }
    })
  }

  private def sendMessage(writer: PrintWriter, messageType: String, message: String): Unit = {
    /*
      Every message has the following format and ends with two line feeds (\n):
      event: [name of event]
      data: [first line]
      data: [second line]
      ...

      See also: https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events#Examples
     */

    var msg = "event: " + messageType.replace("\n", "") + "\n"
    if (message != null)
      msg += "data: " + message.replace("\n", "\ndata: ") + "\n\n"
    writer.write(msg)
    writer.flush()
  }

  get("/", operation(getEvents)) {
    val accept = request.getHeader("Accept")
    if (accept == null || !accept.replace(" ", "").split(",").contains("text/event-stream")) {
      status = 406
    } else {
      authParamRequired {
        contentType = "text/event-stream"

        val asyncContext = request.startAsync()
        asyncContext.setTimeout(0)

        val writer = asyncContext.getResponse.getWriter
        connectionWriters.put(asyncContext, writer)
      }
    }
  }

  private def authParamRequired(func: => Any)(implicit request: HttpServletRequest): Any = {
    val authKeyKey = "authKey"

    if (!request.parameters.contains(authKeyKey) || request.getParameter(authKeyKey).isEmpty) {
      BadRequest()
    } else if (request.getParameter(authKeyKey) != chatOverflow.credentialsService.generateAuthKey()) {
      Unauthorized()
    } else {
      func
    }
  }
}
