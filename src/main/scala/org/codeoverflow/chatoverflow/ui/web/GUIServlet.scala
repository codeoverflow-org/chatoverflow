package org.codeoverflow.chatoverflow.ui.web

import java.io.File
import java.net.URI
import java.util.jar.JarFile

import org.codeoverflow.chatoverflow.WithLogger
import org.eclipse.jetty.http.MimeTypes
import org.eclipse.jetty.util.Loader
import org.scalatra.{ActionResult, ScalatraServlet}

import scala.io.Source

/**
 * A servlet to serve the GUI files of the chatoverflow-gui dir from the classpath.
 * This directory is provided if the gui jar is added on the classpath.
 * Responds with an error if the gui jar isn't on the classpath.
 */
class GUIServlet extends ScalatraServlet with WithLogger {

  private val jarFilePath = {
    val res = Loader.getResource(s"/chatoverflow-gui/")

    // directory couldn't be found
    if (res == null) {
      logger error "GUI couldn't be found on the classpath! Has the GUI been built?"
      None
    } else {
      // remove the path inside the jar and only keep the file path to the jar file
      val jarPath = res.getFile.split("!").head
      logger info s"GUI jar file found at ${new File(".").toURI.relativize(new URI(jarPath))}"

      Some(jarPath)
    }
  }

  get("/*") {
    if (jarFilePath.isEmpty) {
      ActionResult(500, "GUI couldn't be found on the classpath! Has the GUI been built?", Map("Cache-Control" -> "no-cache,no-store"))
    } else {
      val jarFile = new JarFile(new File(new URI(jarFilePath.get)))

      val path = if (requestPath == "/")
        "/index.html"
      else
        requestPath

      val entry = jarFile.getEntry(s"/chatoverflow-gui$path")

      val res = if (entry == null) {
        ActionResult(404, s"Requested file '$path' couldn't be found in the GUI jar!", Map())
      } else {
        contentType = MimeTypes.getDefaultMimeByExtension(entry.getName)
        Source.fromInputStream(jarFile.getInputStream(entry)).mkString
      }

      response.setHeader("Cache-Control", "no-cache,no-store")
      jarFile.close()
      res
    }
  }
}
