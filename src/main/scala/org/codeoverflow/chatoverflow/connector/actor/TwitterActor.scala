package org.codeoverflow.chatoverflow.connector.actor

import akka.actor.Actor
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken}
import org.codeoverflow.chatoverflow.connector.actor.TwitterActor._

import scala.concurrent.Await
import scala.concurrent.duration._


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
      try {
        sender ! TwitterRestClient(ConsumerToken(consumerToken, consumerSecret), AccessToken(accessToken, accessSecret))
      } catch {
        case _: Exception => None
      }

    /*case GetTimeline(client, timeout) =>
      Await.result(client.homeTimeline(count = 1, tweet_mode = TweetMode.Extended), timeout).data.headOption match {
        case Some(t) => {
          sender ! Option(t.text)
        }
        case None => {
          sender ! "Test"
        }
      }*/
    case SendTweet(client, status) =>
      try {
        Await.result(client.createTweet(status), 5 seconds)
        sender ! (true, "")
      } catch {
        case e: Exception => sender ! (false, e)
      }
  }

}

object TwitterActor {

  case class GetRestClient(consumerToken: String, accessToken: String, consumerSecret: String, accessSecret: String) extends ActorMessage

  //case class GetTimeline(client: TwitterRestClient, timeout: Duration) extends ActorMessage

  case class SendTweet(client: TwitterRestClient, status: String) extends ActorMessage

}