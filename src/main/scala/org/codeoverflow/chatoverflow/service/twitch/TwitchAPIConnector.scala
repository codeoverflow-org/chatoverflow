package org.codeoverflow.chatoverflow.service.twitch

import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.configuration.Credentials
import org.codeoverflow.chatoverflow.service.Connector

/**
  * The twitch api connector
  *
  * @param sourceIdentifier the name to the twitch account
  * @param credentials      the credentials to log into the twitch api
  */
class TwitchAPIConnector(override val sourceIdentifier: String, credentials: Credentials) extends Connector(sourceIdentifier, credentials) {
  private val logger = Logger.getLogger(this.getClass)
  var clientID = ""
  var oauth = ""
  val API_FORMAT: String = "application/vnd.twitchtv.v5+json"
  val BASE_URL: String = "https://api.twitch.tv/helix/"
  val BASE_URL_v5: String = "https://api.twitch.tv/kraken/"

  override def getUniqueTypeString: String = this.getClass.getName

  /**
    * Returns true, if the connector has been already instantiated and is running.
    */
  override def isRunning: Boolean = true

  /**
    * Initializes the connector, e.g. creates a connection with its platform.
    */
  override def init(): Unit = {
    val oauth = credentials.getValue(TwitchAPIConnector.credentialsOauthKey)
    val clientID = credentials.getValue(TwitchAPIConnector.credentialsClientID)

    if (clientID.isEmpty) {
      logger warn s"key '${TwitchAPIConnector.credentialsClientID}' not found in credentials for '$sourceIdentifier'."
    } else {
      this.clientID = clientID.get
      if (oauth.isEmpty) {
        logger warn s"key '${TwitchAPIConnector.credentialsOauthKey}' not found in credentials for '$sourceIdentifier'."
      } else {
        this.oauth = oauth.get
      }
    }
  }

  def getSubscriptions(channelID: String, offset: Int = 0, newestFirst: Boolean = true) = {
    get("channels/" + channelID + "/subscriptions", true, true, Seq(("limit", "100"), ("offset", String.valueOf(offset)), ("direction", if (newestFirst) "desc" else "asc")))
  }

  def getUser(userLogin: String) = {
    get("users", false,false, Seq(("login", userLogin)))
  }

  def getFollowers(userID: String) = {
    get("users/follows", false,false, Seq(("to_id", userID)))
  }

  def get(uri: String, auth: Boolean, oldAPI: Boolean, queryParams: Seq[(String, String)]) = {
    val client = HttpClientBuilder.create.build
    val httpGet = if (auth) getUrlAuth(uri, oldAPI) else getURL(uri, oldAPI)
    val urlBuilder = new URIBuilder(httpGet.getURI)
    queryParams.foreach(param => urlBuilder.addParameter(param._1,param._2))
    httpGet.setURI(urlBuilder.build())
    val entity = client.execute(httpGet).getEntity
    if (entity != null) {
      EntityUtils.toString(entity, "UTF-8");
    }
    else ""
  }

  def getURL(uri: String, oldAPI: Boolean) = {
    val baseUrl = if (oldAPI) BASE_URL_v5 else BASE_URL
    new HttpGet(baseUrl + uri) {
      setHeader("Accept", API_FORMAT)
      setHeader("Client-ID", clientID)
    }
  }

  def getUrlAuth(uri: String, oldAPI: Boolean) = {
    val get = getURL(uri, oldAPI)
    get.setHeader("Authorization", oauth)
    get
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