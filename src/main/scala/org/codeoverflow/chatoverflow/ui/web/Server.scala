package org.codeoverflow.chatoverflow.ui.web

import org.codeoverflow.chatoverflow.{ChatOverflow, WithLogger}
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
  context setResourceBase "/"
  context.addEventListener(new ScalatraListener)

  server.setHandler(context)

  /**
    * Starts the server in a new thread.
    */
  def startAsync(): Unit = {
    new Thread(() => startServer()).start()

    println(s"You may open now: http://petstore.swagger.io/?url=http://localhost:$port/api-docs/swagger.json")
    println(s"Or try out the new gui: http://localhost:$port")
  }

  private def startServer(): Unit = {
    server.start()
    server.join()
  }

  /**
    * Stops the server.
    */
  def stop(): Unit = {
    server.stop()
  }

}
