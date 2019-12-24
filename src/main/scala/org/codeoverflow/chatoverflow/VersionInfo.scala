package org.codeoverflow.chatoverflow

import org.codeoverflow.chatoverflow.api.APIVersion._
import org.codeoverflow.chatoverflow.ui.web.GUIServlet

import scala.io.Source
import scala.xml.XML

/**
 * VersionInfo stores the version numbers for the api, framework, rest interface, gui and the used rest interface of the gui.
 */
object VersionInfo extends WithLogger {
  val api: String = s"$MAJOR_VERSION.$MINOR_VERSION"
  val framework: String = {
    try {
      (XML.load(getClass.getResourceAsStream("/dependencies.pom")) \ "version").text
    } catch {
      case _: Exception => "unknown"
    }
  }
  val rest: String = "3.0.0-3"
  val gui: String = getGUIVersionFile("/version_gui.txt")
  val usedRest: String = getGUIVersionFile("/version_gui_rest.txt")

  def logSummary(): Unit = {
    logger info s"Running Chat Overflow version $framework with api version $api."
    logger info s"Providing rest interface with version $rest."
    if (gui != "unknown") {
      logger info s"GUI version $gui using rest interface version $usedRest detected."
    } else {
      logger warn "No GUI detected!"
    }
    if (usedRest != "unknown" && usedRest != rest) {
      logger warn "Provided rest interface version and the used one of the GUI differ. " +
        "GUI functionality may be restricted!"
    }
  }

  private def getGUIVersionFile(name: String): String = try {
    val stream = GUIServlet.getStream(name)
    if (stream.isDefined)
      Source.fromInputStream(stream.get).mkString
    else
      "unknown"
  } catch {
    case _: Exception => "unknown"
  }
}
