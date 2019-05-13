package org.codeoverflow.chatoverflow.requirement.service.twitter

import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.enums.TweetMode
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken, Tweet}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector
import org.codeoverflow.chatoverflow.connector.actor.{GetRestClient, GetTimeline, Privileged, PrivilegedActor, SendTweet, TwitterActor}

import scala.concurrent.Await
import scala.concurrent.duration._

/*
 * TODO: Remove magic values
 * TODO: Implement Actors
 * TODO: Hacktival
 */


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
  protected val timeout: Duration = 5 seconds
  override protected var requiredCredentialKeys: List[String] = List(consumerTokenConfig, consumerSecretConfig, accessSecretConfig, accessTokenConfig)
  override protected var optionalCredentialKeys: List[String] = List()
  private val twitterActor = createActor[TwitterActor]()
  private var client: TwitterRestClient = _

  override def isRunning: Boolean = running


  override def start(): Boolean = {
    client = askActor[TwitterRestClient](twitterActor, -1, GetRestClient(credentials.get.getValue(consumerTokenConfig).get,
      credentials.get.getValue(accessTokenConfig).get, credentials.get.getValue(consumerSecretConfig).get, credentials.get.getValue(accessSecretConfig).get)).get
    true
  }

  def getTimeline(client: TwitterRestClient, timeout: Duration): Option[String] =
    askActor(twitterActor, 5, GetTimeline(client, timeout))

  def sendTweet(client: TwitterRestClient, timeout: Duration, status: String) : Boolean = askActor(twitterActor, 5, SendTweet(client, status, timeout)).get

  def getClient : TwitterRestClient = client

  override def getUniqueTypeString: String = this.getClass.getName


  /**
    * This stops the activity of the connector, e.g. by closing the platform connection.
    */
  override def stop(): Boolean = {
    true
  }
}