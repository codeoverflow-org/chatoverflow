package org.codeoverflow.chatoverflow.requirement.service.twitter

import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * The twitch connector connects to the irc service to work with chat messages.
  * todo fix me
  *
  * @param sourceIdentifier the name to the twitch account
  */
class TwitterConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {
  private val consumerTokenConfig = "consumerToken"
  private val consumerSecretConfig = "consumerSecret"
  private val accessTokenConfig = "accessToken"
  private val accessSecretConfig = "accessSecret"
  override protected var requiredCredentialKeys: List[String] = List(consumerTokenConfig, consumerSecretConfig, accessSecretConfig, accessTokenConfig)
  override protected var optionalCredentialKeys: List[String] = List()
  private var client: TwitterRestClient = _

  override def isRunning: Boolean = running


  override def start(): Boolean = {

    val consumerToken = credentials.get.getValue(consumerTokenConfig).get
    val accessToken = credentials.get.getValue(accessTokenConfig).get

    val consumerSecret = credentials.get.getValue(consumerSecretConfig).get
    val accessSecret = credentials.get.getValue(accessSecretConfig).get

    client = new TwitterRestClient(ConsumerToken(consumerToken, consumerSecret), AccessToken(accessToken, accessSecret))
    true
  }

  def getTimeline: String = {
    Await.result(client.homeTimeline(count = 1), 5 seconds).data.headOption match {
      case Some(t) => t.text
      case None => "None"
    }
  }

  def sendTweet(status: String) = {
    Await.result(client.createTweet(status), 5 seconds)
    true
  }


  override def getUniqueTypeString: String = this.getClass.getName


  /**
    * This stops the activity of the connector, e.g. by closing the platform connection.
    */
  override def stop(): Boolean = {
    true
  }
}