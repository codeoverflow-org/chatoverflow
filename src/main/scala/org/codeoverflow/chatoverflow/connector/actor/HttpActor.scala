package org.codeoverflow.chatoverflow.connector.actor

import akka.actor.Actor
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils

/**
  * The HttpActor can be used to handle http requests.
  */
class HttpActor extends Actor {
  private val client = HttpClientBuilder.create.build

  /**
    * Send a GetRequest-Object to perform a http get request.
    *
    * @return the http request answer as some string or none
    */
  override def receive: Receive = {
    case GetRequest(uri, settings, queryParams) =>
      try {
        var httpGet = new HttpGet(uri)
        httpGet = settings(httpGet)

        val urlBuilder = new URIBuilder(httpGet.getURI)
        queryParams.foreach(param => urlBuilder.addParameter(param._1, param._2))
        httpGet.setURI(urlBuilder.build())

        val entity = client.execute(httpGet).getEntity
        if (entity != null) {
          sender ! Some(EntityUtils.toString(entity, "UTF-8"))
        } else {
          sender ! None
        }
      } catch {
        case _: Exception => None
      }
  }
}

/**
  * A get request consists of a URI at least. Http (e.g. header) settings and query parameters are optional.
  *
  * @param uri         the web address incl. the protocol you want to request
  * @param settings    a function manipulating the generated HttpGet-Element, e.g. by adding header-entries
  * @param queryParams the query params as sequence of key-value-tuple
  */
case class GetRequest(uri: String,
                      settings: HttpGet => HttpGet = httpGet => httpGet,
                      queryParams: Seq[(String, String)] = Seq[(String, String)]()) extends ActorMessage