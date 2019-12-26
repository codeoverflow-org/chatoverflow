package org.codeoverflow.chatoverflow.ui.web

import java.io.{File, InputStream}
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

  import GUIServlet.getStream

  private val isGUIOnClasspath = getStream("/chatoverflow-gui/index.html").isDefined

  get("/*") {
    response.setHeader("Cache-Control", "no-cache,no-store")

    if (!isGUIOnClasspath)
      ActionResult(500, "GUI couldn't be found on the classpath! Has the GUI been built?", Map("Cache-Control" -> "no-cache,no-store"))
    else {
      val path = if (requestPath == "/")
        "/index.html"
      else
        requestPath

      val stream = getStream(s"/chatoverflow-gui$path")

      if (stream.isEmpty) {
        ActionResult(404, s"Requested file '$path' couldn't be found in the GUI classpath directory!", Map())
      } else {
        contentType = MimeTypes.getDefaultMimeByExtension(path)

        val is = stream.get
        val os = response.outputStream

        Iterator.continually(is.read)
          .takeWhile(_ != -1)
          .foreach(os.write)
      }
    }
  }


}

/**
 * This companion object holds a reference to the gui jar file and a method to access the files from the jar
 * or from the class path.
 */
object GUIServlet extends WithLogger {
  private val guiJar: Option[JarFile] = Try {
    val guiJarPath = getClass.getClassLoader.getResource("/chatoverflow-gui").getFile.split("!").head

    logger info s"GUI jar file found at ${new File(".").toURI.relativize(new URI(guiJarPath))}"
    new JarFile(new File(new URI(guiJarPath)))
  }.toOption

  def getStream(path: String): Option[InputStream] = {
    if (guiJar.isEmpty) {
      Option(getClass.getResourceAsStream(path))
    } else {
      val entry = guiJar.get.getEntry(path)
      if (entry != null)
        Option(guiJar.get.getInputStream(entry))
      else
        None
    }
  }
}