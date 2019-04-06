package org.codeoverflow.chatoverflow.requirement.service.twitch.api

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import org.apache.http.HttpEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.util.EntityUtils
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector
import org.codeoverflow.chatoverflow.framework.actors.HttpClientActor

import scala.concurrent.Await
import scala.concurrent.duration._

// FIXME: Chery picked from Class Library Rework, should be reworked, lol

/**
  * The twitch api connector
  *
  * @param sourceIdentifier the name to the twitch account
  */
class TwitchAPIConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {
  private val API_FORMAT: String = "application/vnd.twitchtv.v5+json"
  private val BASE_URL: String = "https://api.twitch.tv/helix/"
  private val BASE_URL_v5: String = "https://api.twitch.tv/kraken/"
  private val actorSystem = ActorSystem("TwitchAPIActorSystem")
  private val actor: ActorRef = actorSystem.actorOf(Props[HttpClientActor])
  private var clientID = ""
  private var oauth = ""
  requiredCredentialKeys = List(TwitchAPIConnector.credentialsClientID, TwitchAPIConnector.credentialsOauthKey)

  override def getUniqueTypeString: String = this.getClass.getName

  /**
    * Returns true, if the connector has been already instantiated and is running.
    */
  override def isRunning: Boolean = true

  /**
    * Initializes the connector, e.g. creates a connection with its platform.
    */
  override def init(): Boolean = {
    val oauth = credentials.get.getValue(TwitchAPIConnector.credentialsOauthKey)
    val clientID = credentials.get.getValue(TwitchAPIConnector.credentialsClientID)

    if (clientID.isEmpty) {
      logger warn s"key '${TwitchAPIConnector.credentialsClientID}' not found in credentials for '$sourceIdentifier'."
      false
    } else {
      this.clientID = clientID.get
      if (oauth.isEmpty) {
        logger warn s"key '${TwitchAPIConnector.credentialsOauthKey}' not found in credentials for '$sourceIdentifier'."
        false
      } else {
        this.oauth = oauth.get
        true
      }
    }
  }

  def getSubscriptions(channelID: String, offset: Int = 0, newestFirst: Boolean = true): String = {
    get("channels/" + channelID + "/subscriptions", auth = true, oldAPI = true, Seq(("limit", "100"), ("offset", String.valueOf(offset)), ("direction", if (newestFirst) "desc" else "asc")))
  }

  def getUser(userLogin: String): String = {
    get("users", auth = false, oldAPI = false, Seq(("login", userLogin)))
  }

  def get(uri: String, auth: Boolean, oldAPI: Boolean, queryParams: Seq[(String, String)]): String = {
    val httpGet = if (auth) getUrlAuth(uri, oldAPI) else getURL(uri, oldAPI)
    val urlBuilder = new URIBuilder(httpGet.getURI)
    queryParams.foreach(param => urlBuilder.addParameter(param._1, param._2))
    httpGet.setURI(urlBuilder.build())
    implicit val timeout: Timeout = Timeout(5 seconds)
    val entity = Await.result(actor ? httpGet, timeout.duration).asInstanceOf[HttpEntity]
    if (entity != null) {
      EntityUtils.toString(entity, "UTF-8")
    }
    else ""
  }

  def getUrlAuth(uri: String, oldAPI: Boolean): HttpGet = {
    val get = getURL(uri, oldAPI)
    get.setHeader("Authorization", oauth)
    get
  }

  def getURL(uri: String, oldAPI: Boolean): HttpGet = {
    val baseUrl = if (oldAPI) BASE_URL_v5 else BASE_URL
    new HttpGet(baseUrl + uri) {
      setHeader("Accept", API_FORMAT)
      setHeader("Client-ID", clientID)
    }
  }

  def getFollowers(userID: String): String = {
    get("users/follows", auth = false, oldAPI = false, Seq(("to_id", userID)))
  }

  /**
    * Shuts down the connector, closes its platform connection.
    */
  override def shutdown(): Unit = ???
}

object TwitchAPIConnector {
  val credentialsOauthKey = "oauth"
  val credentialsClientID = "clientid"
}