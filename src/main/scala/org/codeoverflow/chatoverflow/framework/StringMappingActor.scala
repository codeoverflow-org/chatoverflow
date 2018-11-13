package org.codeoverflow.chatoverflow.framework

import akka.actor.Actor

case class Mapping(mapFunc: (String => Any), content: String)

class StringMappingActor extends Actor {

  override def receive: Receive = {
    case m: Mapping => sender ! m.mapFunc(m.content)
  }
}
