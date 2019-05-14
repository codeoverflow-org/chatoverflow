package org.codeoverflow.chatoverflow.connector.actor

import akka.actor.Actor
import org.codeoverflow.chatoverflow.connector.actor.JsonActor.ParseJSON
import org.json4s._
import org.json4s.jackson.JsonMethods._

/**
  * The JSON Actor uses json4s to parse serialized json and enable traversing.
  */
class JsonActor extends Actor {

  // TODO: Implement proper functionality with json4s. This is not more than a proof of concept
  // TODO: Test, when reflection kicks in to allow the most functionality without an actor call

  /**
    * Receives a ParseJSON Object to start parsing.
    *
    * @return the desired values, can be any type
    */
  override def receive: Receive = {
    case ParseJSON(json, parseMethod) =>
      val rootObject = parse(json)
      sender ! parseMethod(rootObject)
  }
}

object JsonActor {

  /**
    * Send a ParseJSON-object to start parsing. The traversal is custom.
    *
    * @param json        the serialized json string
    * @param parseMethod a function what should happen with the parsed json. Return type is any
    */
  case class ParseJSON(json: String, parseMethod: JValue => Any) extends ActorMessage

}