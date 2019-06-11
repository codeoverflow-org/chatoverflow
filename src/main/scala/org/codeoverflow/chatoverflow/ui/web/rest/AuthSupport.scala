package org.codeoverflow.chatoverflow.ui.web.rest

import org.scalatra.swagger.{SwaggerSupport, SwaggerSupportSyntax}

trait AuthSupport extends SwaggerSupport {

  protected def authHeader: SwaggerSupportSyntax.ParameterBuilder[String] =
    headerParam[String]("authKey").description("connection auth key required")

}
