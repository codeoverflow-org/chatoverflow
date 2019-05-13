package org.codeoverflow.chatoverflow.ui.web

import org.codeoverflow.chatoverflow.Launcher
import org.json4s.JsonDSL._
import org.json4s.{JValue, _}
import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{Api, ApiInfo, JacksonSwaggerBase, Swagger}

class OpenAPIServlet(implicit val swagger: Swagger) extends ScalatraServlet with JacksonSwaggerBase {

  /**
    * This implementation adds a host and schemes argument to the swagger documentation.
    */
  override def renderSwagger2(docs: List[Api]): JValue = {
    val swagger2 = super.renderSwagger2(docs)
    val schemes: JObject = "schemes" -> List("http")
    val host: JObject = "host" -> s"localhost:${Launcher.server.get.port}"
    val additionalElements = schemes ~ host
    additionalElements merge swagger2
  }
}

object CodeOverflowApiInfo extends ApiInfo(
  "Code Overflow API",
  "This API is the main entry point of the Chat Overflow GUI and third party projects.",
  "http://codeoverflow.org",
  "hi@sebinside.de",
  "Eclipse Public License 2.0",
  "https://github.com/codeoverflow-org/chatoverflow/blob/master/LICENSE")

class CodeOverflowSwagger(apiVersion: String) extends Swagger(Swagger.SpecVersion, apiVersion, CodeOverflowApiInfo)