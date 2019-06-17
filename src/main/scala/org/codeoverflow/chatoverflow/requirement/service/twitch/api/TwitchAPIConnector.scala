package org.codeoverflow.chatoverflow.requirement.service.twitch.api

import akka.actor.ActorRef
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

/**
  * The twitch api connector
  *
  * @param sourceIdentifier the name to the twitch account
  */
class TwitchAPIConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {
  private val API_FORMAT: String = "application/vnd.twitchtv.v5+json"
  private val BASE_URL: String = "https://api.twitch.tv/helix/"
  private val BASE_URL_v5: String = "https://api.twitch.tv/kraken/"
  private val actor: ActorRef = createActor[HttpClientActor]()
  override protected var requiredCredentialKeys: List[String] = List(TwitchAPIConnector.credentialsClientID, TwitchAPIConnector.credentialsOauthKey)
  private var clientID = ""
  private var oauth = ""

  override def getUniqueTypeString: String = this.getClass.getName

  /**
    * Returns true, if the connector has been already instantiated and is running.
    */
  override def isRunning: Boolean = true

  def getSubscriptions(channelID: String, offset: Int = 0, newestFirst: Boolean = true): String = {
    get("channels/" + channelID + "/subscriptions", auth = true, oldAPI = true, Seq(("limit", "100"), ("offset", String.valueOf(offset)), ("direction", if (newestFirst) "desc" else "asc")))
  }

  def getUser(userLogin: String): String = {
    get("users", auth = false, oldAPI = false, Seq(("login", userLogin)))
  }

  def getFollowers(userID: String): String = {
    get("users/follows", auth = false, oldAPI = false, Seq(("to_id", userID)))
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

  /**
    * Starts the connector, e.g. creates a connection with its platform.
    */
  override def start(): Boolean = {
    oauth = credentials.get.getValue(TwitchAPIConnector.credentialsOauthKey).get
    clientID = credentials.get.getValue(TwitchAPIConnector.credentialsClientID).get
    true
  }

  /**
    * This stops the activity of the connector, e.g. by closing the platform connection.
    */
  override def stop(): Boolean = {
    false
  }

  override protected var optionalCredentialKeys: List[String] = List()
}

object TwitchAPIConnector {
  val credentialsOauthKey = "oauth"
  val credentialsClientID = "clientid"
}