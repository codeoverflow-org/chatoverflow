package org.codeoverflow.chatoverflow.ui.web.rest.events

import org.codeoverflow.chatoverflow.ui.web.rest.{AuthSupport, TagSupport}
import org.scalatra.swagger.{SwaggerSupport, SwaggerSupportSyntax}
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder

trait EventsControllerDefinition extends SwaggerSupport with TagSupport with AuthSupport {
  val getEvents: OperationBuilder =
    (apiOperation[Object]("getEvents")
      summary "Get events"
      description "Get events from chatoverflow using the EventSource API. Requires an Accept-header with the value text/event-stream."
      parameter authQuery
      tags controllerTag)

  protected def authQuery: SwaggerSupportSyntax.ParameterBuilder[String] =
    queryParam[String]("authKey").description("connection auth key required")

  override def controllerTag: String = "events"

  override protected def applicationDescription: String = "Handles chatoverflow events."
}
