package org.codeoverflow.chatoverflow.requirement.service.twitter

import com.danielasfregola.twitter4s.TwitterRestClient
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector
import org.codeoverflow.chatoverflow.connector.actor.TwitterActor
import org.codeoverflow.chatoverflow.connector.actor.TwitterActor._

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
  private var client: TwitterRestClient = _
  override protected var requiredCredentialKeys: List[String] = List(consumerTokenConfig, consumerSecretConfig, accessSecretConfig, accessTokenConfig)
  override protected var optionalCredentialKeys: List[String] = List()
  private val twitterActor = createActor[TwitterActor]()

  def sendTweet(status: String): (Boolean, String) = twitterActor.??[(Boolean, String)](5){SendTweet(client, status)}.get

  /*def getTimeline(client: TwitterRestClient, timeout: Duration): Option[String] =
    askActor(TwitterActor, 5, GetTimeline(client, timeout))*/

  def getClient: TwitterRestClient = client

  override def getUniqueTypeString: String = this.getClass.getName

  override def start(): Boolean = {
    logger info s"Starting twitter connector! Source identifier is: '$sourceIdentifier'."
    this.client = twitterActor.??[TwitterRestClient](5){GetRestClient(credentials.get.getValue(consumerTokenConfig).get,
      credentials.get.getValue(accessTokenConfig).get, credentials.get.getValue(consumerSecretConfig).get, credentials.get.getValue(accessSecretConfig).get)}.get
    true
  }

  /**
    * This stops the activity of the connector, e.g. by closing the platform connection.
    */
  override def stop(): Boolean = {
    true
  }
}