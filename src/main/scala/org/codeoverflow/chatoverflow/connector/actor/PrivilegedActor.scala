package org.codeoverflow.chatoverflow.connector.actor

import akka.actor.Actor
import org.codeoverflow.chatoverflow.connector.actor.PrivilegedActor.Privileged

/**
  * The privileged actor is the most general idea of an actor to handle privileged actions.
  * Use a more specific actor, if possible.
  */
class PrivilegedActor extends Actor {
  /**
    * Handles any function to call. Takes a Privileged containing any function and the arguments to pass.
    * Example: <code>Privileged((aNumber: Int) => s"I got: $aNumber", 42)</code>
    *
    * @return the result type of the function, specified in the message. Can be anything.
    */
  override def receive: Receive = {
    case Privileged(function, args) => sender ! function(args)
  }
}

object PrivilegedActor {

  /**
    * Send a Privileged-ojbect to the PrivilegedActor to get the function executed with the given args.
    *
    * @param function a function of type T => Any
    * @param args     the args for the function of type T
    */
  case class Privileged(function: Any => Any, args: Any) extends ActorMessage

}
