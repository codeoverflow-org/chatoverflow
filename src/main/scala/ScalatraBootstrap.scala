import javax.servlet.ServletContext
import org.codeoverflow.chatoverflow.web.rest.PluginExampleServerlet
import org.scalatra._

/**
  * This class provides all runtime information for Scalatra. Servlets are mounted here.
  */
class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new PluginExampleServerlet(), "/*")
  }
}