package org.codeoverflow.chatoverflow.connector.actor

import akka.actor.Actor
import com.google.gson.{JsonObject, JsonParser}
import org.codeoverflow.chatoverflow.connector.actor.JsonActor.ParseJSON

/**
  * The JSON Actor uses Google GSON to parse serialized json and enable traversing.
  */
class JsonActor extends Actor {
  val parser = new JsonParser

  /**
    * Receives a ParseJSON Object to start parsing.
    *
    * @return the desired values, can be any type
    */
  override def receive: Receive = {
    case ParseJSON(json, parse) =>
      val rootObject = parser.parse(json).getAsJsonObject
      parse(rootObject)
  }
}

object JsonActor {

  /**
    * Send a ParseJSON-object to start parsing. The traversal is custom.
    *
    * @param json  the serialized json string
    * @param parse a function what should happen with the parsed json. Return type is any
    */
  case class ParseJSON(json: String, parse: JsonObject => Any) extends ActorMessage

}