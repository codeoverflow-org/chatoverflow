package org.codeoverflow.chatoverflow.ui.web

import org.codeoverflow.chatoverflow.{ChatOverflow, WithLogger}
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

/**
  * The server runs on the specified port. Servlets are injected from the ScalatraBootstrap Class.
  *
  * @param port         the port to run on the localhost
  * @param chatOverflow the main chat overflow object
  */
class Server(val chatOverflow: ChatOverflow, val port: Int) extends WithLogger {

  private val server = new org.eclipse.jetty.server.Server(port)
  private val context = new WebAppContext()
  context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")
  context setContextPath "/"
  context.setBaseResource(Resource.newClassPathResource("/chatoverflow-gui/"))
  context.addEventListener(new ScalatraListener)

  server.setHandler(context)

  /**
    * Starts the server in a new thread.
    */
  def startAsync(): Unit = {
    // TODO: Enable shutting down the server
    new Thread(() => startServer()).start()

    println(s"You may open now: http://petstore.swagger.io/?url=http://localhost:$port/api-docs/swagger.json")
    println("Or try out the new gui: http://localhost:4200")
  }

  private def startServer(): Unit = {
    server.start()
    server.join()
  }

}
