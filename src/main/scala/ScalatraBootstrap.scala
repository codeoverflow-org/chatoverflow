import javax.servlet.ServletContext
import org.codeoverflow.chatoverflow.ui.web.rest.config.ConfigController
import org.codeoverflow.chatoverflow.ui.web.rest.connector.ConnectorController
import org.codeoverflow.chatoverflow.ui.web.rest.{PluginInstanceController, TypeController}
import org.codeoverflow.chatoverflow.ui.web.{CodeOverflowSwagger, OpenAPIServlet}
import org.scalatra._

/**
  * This class provides all runtime information for Scalatra. Servlets are mounted here.
  */
class ScalatraBootstrap extends LifeCycle {
  val apiVersion = "1.0"
  implicit val swagger: CodeOverflowSwagger = new CodeOverflowSwagger(apiVersion)

  override def init(context: ServletContext) {
    // Allow CORS
    context.initParameters("org.scalatra.cors.allowedOrigins") = "*"
    context.initParameters("org.scalatra.cors.allowCredentials") = "false"

    // Add all servlets and controller
    context.mount(new TypeController(), "/types/*", "types")
    context.mount(new ConfigController(), "/config/*", "config")
    context.mount(new PluginInstanceController(), "/instances/*", "instances")
    context.mount(new ConnectorController(), "/connectors/*", "connectors")
    context.mount(new OpenAPIServlet(), "/api-docs")
  }
}