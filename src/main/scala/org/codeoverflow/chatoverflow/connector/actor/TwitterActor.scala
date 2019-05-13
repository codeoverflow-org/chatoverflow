package org.codeoverflow.chatoverflow.connector.actor

import java.io.PrintWriter

import akka.actor.Actor
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.enums.TweetMode
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken, Tweet}

import scala.concurrent.Await
import scala.concurrent.duration.Duration


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
    case GetRestClient(consumerToken, accessToken, consumerSecret, accessSecret) => try {
      sender ! TwitterRestClient(ConsumerToken(consumerToken, consumerSecret), AccessToken(accessToken, accessSecret))
    } catch {
      case _: Exception => None
    }

    case GetTimeline(client, timeout) =>
      Await.result(client.homeTimeline(count = 1, tweet_mode = TweetMode.Extended), timeout).data.headOption match {
        case Some(t) => Option(t.text)
        case None => None
      }
    case SendTweet(client, status, timeout) =>
      Await.result(client.createTweet(status), timeout)
      sender ! true
  }

}

case class GetRestClient(consumerToken: String, accessToken: String, consumerSecret: String, accessSecret: String)

case class GetTimeline(client: TwitterRestClient, timeout: Duration)

case class SendTweet(client: TwitterRestClient, status: String, timeout: Duration)