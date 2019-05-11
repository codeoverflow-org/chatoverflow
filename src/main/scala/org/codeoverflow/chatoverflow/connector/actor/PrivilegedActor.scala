package org.codeoverflow.chatoverflow.connector.actor

import akka.actor.Actor

/**
  * The privileged actor is the most general idea of an actor to handle privileged actions.
  * Use a more specific actor, if possible.
  */
class PrivilegedActor extends Actor {
  /**
    * Handles any function to call. Takes a tuple of any argument and any function and the arguments to pass.
    * Example: <code>((aNumber: Int) => s"I got: $aNumber", 42)</code>
    *
    * @return the result type of the function, specified in the message. Can be anything.
    */
  override def receive: Receive = {
    case message: (((Any) => Any), Any) => sender ! message._1(message._2)
  }
}
