package org.codeoverflow.chatoverflow.ui.web

import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{ApiInfo, JacksonSwaggerBase, Swagger}

class OpenAPIServlet(implicit val swagger: Swagger) extends ScalatraServlet with JacksonSwaggerBase

object CodeOverflowApiInfo extends ApiInfo(
  "Code Overflow API",
  "This API is the main entry point of the Chat Overflow GUI and third party projects",
  "http://codeoverflow.org",
  "hi@sebinside.de",
  "Eclipse Public License 2.0",
  "https://github.com/codeoverflow-org/chatoverflow/blob/master/LICENSE")

class CodeOverflowSwagger(apiVersion: String) extends Swagger(Swagger.SpecVersion, apiVersion, CodeOverflowApiInfo)
