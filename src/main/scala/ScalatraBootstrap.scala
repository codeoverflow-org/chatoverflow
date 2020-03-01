import javax.servlet.ServletContext
import org.codeoverflow.chatoverflow.VersionInfo
import org.codeoverflow.chatoverflow.ui.web.rest.config.ConfigController
import org.codeoverflow.chatoverflow.ui.web.rest.connector.ConnectorController
import org.codeoverflow.chatoverflow.ui.web.rest.events.{EventsController, EventsDispatcher}
import org.codeoverflow.chatoverflow.ui.web.rest.plugin.PluginInstanceController
import org.codeoverflow.chatoverflow.ui.web.rest.types.TypeController
import org.codeoverflow.chatoverflow.ui.web.{CodeOverflowSwagger, GUIServlet, OpenAPIServlet}
import org.scalatra._

/**
 * This class provides all runtime information for Scalatra. Servlets are mounted here.
 */
class ScalatraBootstrap extends LifeCycle {
  implicit val swagger: CodeOverflowSwagger = new CodeOverflowSwagger(VersionInfo.rest)

  override def init(context: ServletContext) {

    // Allow CORS
    context.setInitParameter("org.scalatra.cors.allowedOrigins", "*")
    context.setInitParameter("org.scalatra.cors.allowCredentials", "false")
    context.setInitParameter("org.scalatra.cors.allowedMethods", "*")

    // Add all servlets and controller
    val eventsController = new EventsController()
    EventsDispatcher.init(eventsController)
    context.mount(eventsController, "/events/*", "events")
    context.mount(new TypeController(), "/types/*", "types")
    context.mount(new ConfigController(), "/config/*", "config")
    context.mount(new PluginInstanceController(), "/instances/*", "instances")
    context.mount(new ConnectorController(), "/connectors/*", "connectors")
    context.mount(new OpenAPIServlet(), "/api-docs")

    context.mount(new GUIServlet(), "/*")
  }
}