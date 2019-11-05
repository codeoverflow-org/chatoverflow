package org.codeoverflow.chatoverflow.ui.web

import java.io.{BufferedInputStream, File}
import java.net.URI
import java.util.jar.JarFile

import org.codeoverflow.chatoverflow.WithLogger
import org.eclipse.jetty.http.MimeTypes
import org.scalatra.{ActionResult, ScalatraServlet}

import scala.util.Try

/**
 * A servlet to serve the GUI files of the chatoverflow-gui dir from the classpath.
 * This directory is provided if the gui jar is added on the classpath.
 * Responds with an error if the gui jar isn't on the classpath.
 */
class GUIServlet extends ScalatraServlet with WithLogger {

  import GUIServlet.guiJar

  get("/*") {
    if (guiJar.isEmpty) {
      ActionResult(500, "GUI couldn't be found on the classpath! Has the GUI been built?", Map("Cache-Control" -> "no-cache,no-store"))
    } else {
      val path = if (requestPath == "/")
        "/index.html"
      else
        requestPath

      val entry = guiJar.get.getEntry(s"/chatoverflow-gui$path")

      val res = if (entry == null) {
        ActionResult(404, s"Requested file '$path' couldn't be found in the GUI jar!", Map())
      } else {
        contentType = MimeTypes.getDefaultMimeByExtension(entry.getName)
        val is = new BufferedInputStream(guiJar.get.getInputStream(entry))
        val os = response.getOutputStream

        Iterator.continually(is.read)
          .takeWhile(_ != -1)
          .foreach(os.write)
      }

      response.setHeader("Cache-Control", "no-cache,no-store")
      res
    }
  }
}

/**
 * This companion object holds a reference to the gui jar file.
 */
object GUIServlet extends WithLogger {
  val guiJar: Option[JarFile] = Try {
    val guiJarPath = getClass.getClassLoader.getResource("/chatoverflow-gui").getFile.split("!").head

    logger info s"GUI jar file found at ${new File(".").toURI.relativize(new URI(guiJarPath))}"
    new JarFile(new File(new URI(guiJarPath)))
  }.toOption
}