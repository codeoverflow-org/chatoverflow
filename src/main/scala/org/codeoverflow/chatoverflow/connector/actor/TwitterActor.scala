package org.codeoverflow.chatoverflow.connector.actor

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import akka.actor.Actor
import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.MediaType.NotCompressible
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken}
import javax.imageio.ImageIO
import org.codeoverflow.chatoverflow.connector.actor.TwitterActor._

import scala.concurrent.Await
import scala.concurrent.duration._


/**
  * The TwitterActor enables communication with the twitter REST-API over the twitter4s library
  */
class TwitterActor extends Actor {

  /**
    * Receives either GetRestClient or a SendTextTweet object, acts accordingly.
    *
    * @return a TwitterRestClient or a (Boolean, String) as result for API interaction
    */
  override def receive: Receive = {
    case GetRestClient(consumerToken, accessToken, consumerSecret, accessSecret) =>
      try {
        sender ! TwitterRestClient(ConsumerToken(consumerToken, consumerSecret), AccessToken(accessToken, accessSecret))
      } catch {
        case _: Exception => None
      }
    case SendTextTweet(client, status) =>
      try {
        Await.result(client.createTweet(status), 5 seconds)
        sender ! (true, "")
      } catch {
        case e: Exception => sender ! (false, e)
      }
    case SendImageTweet(client, status, image) =>
      try {
        val os = new ByteArrayOutputStream()
        ImageIO.write(image, "png", os)
        val bytes = os.toByteArray
        val is = new ByteArrayInputStream(bytes)
        val mediaType = MediaType.image("png", NotCompressible, "png")
        val upload = Await.result(client.uploadMediaFromInputStream(is, bytes.length, mediaType), 5 seconds)
        Await.result(client.createTweet(status, media_ids = Seq(upload.media_id)), 5 second)
        sender ! (true, "")
      } catch {
        case e: Exception => sender ! (false, e)
      }
  }

}

object TwitterActor {

  /**
    * Initiate a new Rest-Client with the needed API-Keys
    *
    * @param consumerToken  Twitter consumer API key
    * @param accessToken    Twitter access token
    * @param consumerSecret Twitter consumer API secret key
    * @param accessSecret   Twitter access token secret
    */
  case class GetRestClient(consumerToken: String, accessToken: String, consumerSecret: String, accessSecret: String) extends ActorMessage

  /**
    * Sends a plaintext tweet for the account the client is connected to
    *
    * @param client TwitterRestClient for the account the tweet should be posted on
    * @param status Text that should be tweeted
    */
  case class SendTextTweet(client: TwitterRestClient, status: String) extends ActorMessage

  /**
   * Sends a tweet with an image for the account the client is connected to
   *
   * @param client TwitterRestClient for the account the tweet should be posted on
   * @param status Text that should be tweeted
   * @param image  BufferedImage that should be tweeted
   */
  case class SendImageTweet(client: TwitterRestClient, status: String, image: BufferedImage) extends ActorMessage

}
