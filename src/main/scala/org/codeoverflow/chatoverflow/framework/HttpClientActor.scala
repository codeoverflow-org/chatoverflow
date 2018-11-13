package org.codeoverflow.chatoverflow.framework

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder

class HttpClientActor extends Actor {
  private val client = HttpClientBuilder.create.build

  override def receive: Receive = {
    case httpGet: HttpGet => sender ! client.execute(httpGet).getEntity
  }
}
