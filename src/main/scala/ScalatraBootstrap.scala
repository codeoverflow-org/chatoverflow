import javax.servlet.ServletContext
import org.codeoverflow.chatoverflow.ui.web.rest.{ConfigServlet, PluginInstanceServlet, TypeServlet}
import org.scalatra._

/**
  * This class provides all runtime information for Scalatra. Servlets are mounted here.
  */
class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new TypeServlet(), "/types/*")
    context.mount(new ConfigServlet(), "/config/*")
    context.mount(new PluginInstanceServlet(), "/instances/*")
  }
}