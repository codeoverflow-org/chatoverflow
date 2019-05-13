import javax.servlet.ServletContext
import org.codeoverflow.chatoverflow.ui.web.rest.config.ConfigServlet
import org.codeoverflow.chatoverflow.ui.web.rest.{ConnectorServlet, PluginInstanceServlet, TypeServlet}
import org.codeoverflow.chatoverflow.ui.web.{CodeOverflowSwagger, OpenAPIServlet}
import org.scalatra._

/**
  * This class provides all runtime information for Scalatra. Servlets are mounted here.
  */
class ScalatraBootstrap extends LifeCycle {
  val apiVersion = "1.0"
  implicit val swagger: CodeOverflowSwagger = new CodeOverflowSwagger(apiVersion)

  override def init(context: ServletContext) {
    context.mount(new TypeServlet(), "/types/*", "types")
    context.mount(new ConfigServlet(), "/config/*", "config")
    context.mount(new PluginInstanceServlet(), "/instances/*", "instances")
    context.mount(new ConnectorServlet(), "/connectors/*", "connectors")
    context.mount(new OpenAPIServlet(), "/api-docs")
  }
}