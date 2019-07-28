package org.codeoverflow.chatoverflow.requirement.service.twitter

import com.danielasfregola.twitter4s.TwitterRestClient
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector
import org.codeoverflow.chatoverflow.connector.actor.TwitterActor
import org.codeoverflow.chatoverflow.connector.actor.TwitterActor._


/**
  * The twitter connector connects to the twitter REST-API to send tweets
  *
  * @param sourceIdentifier is not in use so can be set freely
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

  def sendTextTweet(status: String): (Boolean, String) = twitterActor.??[(Boolean, String)](5) {
    SendTextTweet(client, status)
  }.get

  def getClient: TwitterRestClient = client

  override def getUniqueTypeString: String = this.getClass.getName

  /**
    * This starts the connector by initiating the twitter REST client.
    */
  override def start(): Boolean = {
    logger info s"Starting twitter connector!"
    this.client = twitterActor.??[TwitterRestClient](5) {
      GetRestClient(credentials.get.getValue(consumerTokenConfig).get,
        credentials.get.getValue(accessTokenConfig).get, credentials.get.getValue(consumerSecretConfig).get, credentials.get.getValue(accessSecretConfig).get)
    }.get
    true
  }

  /**
    * This stops the activity of the connector, e.g. by closing the platform connection.
    */
  override def stop(): Boolean = {
    logger info s"Stopping the twitter connector"
    this.client.shutdown()
    true
  }
}