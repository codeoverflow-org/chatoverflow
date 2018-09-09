import javax.servlet.ServletContext
import org.codeoverflow.chatoverflow.ui.web.{ApiServlet, MainServlet}
import org.scalatra._

/**
  * This class provides all runtime information for Scalatra. Servlets are mounted here.
  */
class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new ApiServlet(), "/api/*")
    context.mount(new MainServlet(), "/*")
  }
}