package org.codeoverflow.chatoverflow.ui.web

import org.codeoverflow.chatoverflow.ChatOverflow
import org.eclipse.jetty.servlet.ServletHandler.Default404Servlet
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

/**
  * The server runs on the specified port. Servlets are injected from the ScalatraBootstrap Class.
  *
  * @param port         the port to run on the localhost
  * @param chatOverflow the main chat overflow object
  */
class Server(val chatOverflow: ChatOverflow, val port: Int) {

  private val server = new org.eclipse.jetty.server.Server(port)
  private val context = new WebAppContext()
  context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")
  context setContextPath "/"
  context.setResourceBase("/")
  context.addEventListener(new ScalatraListener)
  context.addServlet(classOf[Default404Servlet], "/")

  server.setHandler(context)

  /**
    * Starts the server in a new thread.
    */
  def startAsync(): Unit = {
    // TODO: Enable shutting down the server
    new Thread(() => startServer()).start()
  }

  private def startServer(): Unit = {
    server.start()
    server.join()
  }

}
