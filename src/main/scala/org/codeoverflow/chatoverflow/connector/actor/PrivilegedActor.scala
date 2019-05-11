package org.codeoverflow.chatoverflow.connector.actor

import akka.actor.Actor

class PrivilegedActor extends Actor {
  override def receive: Receive = {
    case function: (Any => Any) => function
  }
}
