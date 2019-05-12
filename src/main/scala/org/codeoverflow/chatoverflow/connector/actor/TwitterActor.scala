package org.codeoverflow.chatoverflow.connector.actor

import java.io.PrintWriter

import akka.actor.Actor
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken}


/**
  * The file system actor provides simple utility methods to read and write files.
  */
class TwitterActor extends Actor {

  /**
    * Receives either LoadFile or SaveFile object, acts accordingly.
    *
    * @return a loaded file or a boolean if the saving process was successful
    */
  override def receive: Receive = {
    case GetRestClient(consumerToken, accessToken, consumerSecret, accessSecret) =>
      TwitterRestClient(ConsumerToken(consumerToken, consumerSecret), AccessToken(accessToken, accessSecret))
  }

}

case class GetRestClient(consumerToken: String, accessToken: String, consumerSecret: String, accessSecret: String)

case class GetTimeline()

case class LoadFile(pathInResources: String)

case class SendTweet(status: String)