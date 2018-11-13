package org.codeoverflow.chatoverflow.framework

case class Mapping(mapFunc: (String => Any), content: String)

class StringMappingActor extends Actor {

  override def receive: Receive = {
    case m: Mapping => sender ! m.mapFunc(m.content)
  }
}
