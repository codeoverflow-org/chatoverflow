package org.codeoverflow.chatoverflow.requirement.service.twitter

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken}
/**
  * The twitch connector connects to the irc service to work with chat messages.
  * todo fix me
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

      val consumerToken = credentials.get.getValue(consumerTokenConfig)
      val accessToken = credentials.get.getValue(accessTokenConfig)

      val consumerSecret = credentials.get.getValue(consumerSecretConfig)
      val accessSecret = credentials.get.getValue(accessSecretConfig)

      //fixme: orNull may crash the Connector
      client = new TwitterRestClient(ConsumerToken(consumerToken.orNull, consumerSecret.orNull), AccessToken(accessToken.orNull, accessSecret.orNull))
      true
  }


  override def getUniqueTypeString: String = this.getClass.getName


  /**
    * This stops the activity of the connector, e.g. by closing the platform connection.
    */
  override def stop(): Boolean = { true }
}