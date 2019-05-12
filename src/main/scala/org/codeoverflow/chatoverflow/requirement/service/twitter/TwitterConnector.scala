package org.codeoverflow.chatoverflow.requirement.service.twitter

import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.enums.TweetMode
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken, Tweet}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector
import org.codeoverflow.chatoverflow.connector.actor.{Privileged, PrivilegedActor}

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
  private val privilegedActor = createActor[PrivilegedActor]()
  private var client: TwitterRestClient = _

  override def isRunning: Boolean = running


  override def start(): Boolean = {
    client = askActor[TwitterRestClient](privilegedActor, -1, getRestClient).get
    true
  }

  private def getRestClient: TwitterRestClient = {
    val consumerToken = credentials.get.getValue(consumerTokenConfig).get
    val accessToken = credentials.get.getValue(accessTokenConfig).get

    val consumerSecret = credentials.get.getValue(consumerSecretConfig).get
    val accessSecret = credentials.get.getValue(accessSecretConfig).get

    TwitterRestClient(ConsumerToken(consumerToken, consumerSecret), AccessToken(accessToken, accessSecret))
  }

  def getTimeline: Option[String] = askActor(privilegedActor, 5, {
    Await.result(client.homeTimeline(count = 1, tweet_mode = TweetMode.Extended), timeout).data.headOption match {
      case Some(t) => Option(t.text)
      case None => None
    }
  })

  def sendTweet(status: String) = {
    Await.result(client.createTweet(status), timeout)
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