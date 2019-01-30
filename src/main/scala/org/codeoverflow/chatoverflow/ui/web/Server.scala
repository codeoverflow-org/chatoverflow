package org.codeoverflow.chatoverflow.ui.web

import java.awt.Desktop
import java.net.URL

import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

/**
  * The server runs on the specified port. Servlets are injected from the ScalatraBootstrap Class.
  *
  * @param port the port to run on the localhost
  */
class Server(val port: Int) {

  private val server = new org.eclipse.jetty.server.Server(port)
  private val context = new WebAppContext()

  private val serverUrl = s"http://localhost:$port"

  context.setContextPath("/")
  context.setResourceBase("src/main/webapp")
  context.addEventListener(new ScalatraListener)
  context.addServlet(classOf[DefaultServlet], "/")

  server.setHandler(context)

  /**
    * Starts the server in a new thread.
    */
  def startAsync(): Unit = {
    new Thread(() => startServer()).start()
  }

  private def startServer(): Unit = {
    server.start()
    Desktop.getDesktop.browse(new URL(serverUrl).toURI)
    server.join()
  }

}
