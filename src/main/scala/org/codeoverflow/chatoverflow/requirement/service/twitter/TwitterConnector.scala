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
  private var running = false
  private var client: TwitterRestClient = _

  override def isRunning: Boolean = running

  override def init(): Boolean = {
    if (!running) {
      logger info s"Starting connector for source '$sourceIdentifier' of type '$getUniqueTypeString'."

      if (!areCredentialsSet) {
        logger warn "No credentials set."
        false
      } else {
        client = getClient()
        running = true
        logger info "Started connector."
        true
      }
    }
    else {
      logger warn "Connector already running."
      false
    }

  }

  private def getClient(): TwitterRestClient = {

    //if (credentials.isDefined) {

      val consumerToken = credentials.get.getValue(consumerTokenConfig)
      val accessToken = credentials.get.getValue(accessTokenConfig)

      val consumerSecret = credentials.get.getValue(consumerSecretConfig)
      val accessSecret = credentials.get.getValue(accessSecretConfig)

      // todo: refactor this
      if (consumerToken.isEmpty) {
        logger warn s"key '$consumerToken' not found in credentials for '$sourceIdentifier'."
      }
      if (consumerSecret.isEmpty) {
        logger warn s"key '$consumerSecret' not found in credentials for '$sourceIdentifier'."
      }
      if (accessToken.isEmpty) {
        logger warn s"key '$accessToken' not found in credentials for '$sourceIdentifier'."
      }
      if (accessSecret.isEmpty) {
        logger warn s"key '$accessSecret' not found in credentials for '$sourceIdentifier'."
      }

      //fixme: orNull may crash the Connector
      new TwitterRestClient(ConsumerToken(consumerToken.orNull, consumerSecret.orNull), AccessToken(accessToken.orNull, accessSecret.orNull))

//    }
  }


  override def getUniqueTypeString: String = this.getClass.getName

  /**
    * {@inheritdoc}
    */
  override def shutdown(): Unit = logger info s"Stopped connector for source '$sourceIdentifier' of type '$getUniqueTypeString'."

}