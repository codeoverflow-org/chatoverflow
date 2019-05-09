package org.codeoverflow.chatoverflow.requirement.service.twitch.api

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneOffset}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.google.gson.JsonParser
import org.apache.http.HttpEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.util.EntityUtils
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.input.chat._
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
  private val BASE_URL: String = "https://api.twitch.tv/v5/"
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

  def getVideo(videoID: String): String = {
    get(s"videos/$videoID", auth = false, oldAPI = false, Seq())
  }

  def getVideoComments(videoID: String): java.util.List[TwitchChatMessage] = {
    val formatter = DateTimeFormatter.ISO_INSTANT
    val messages = new java.util.ArrayList[TwitchChatMessage]()
    val parser = new JsonParser()

    var comments = get(s"videos/$videoID/comments", auth = false, oldAPI = true, Seq())
    var jsonElement = parser.parse(comments)
    var jsonObject = jsonElement.getAsJsonObject

    var hasNext = true
    while (hasNext) {
      val jsonArray = jsonObject.getAsJsonArray("comments")
      jsonArray.forEach(e => {
        val jsonCommentObject = e.getAsJsonObject
        val channelID: String = jsonCommentObject.getAsJsonPrimitive("channel_id").getAsString
        val timestamp: Long = Instant.from(formatter.parse(jsonCommentObject.getAsJsonPrimitive("created_at").getAsString)).toEpochMilli
        val timeOffset: Double = jsonCommentObject.getAsJsonPrimitive("content_offset_seconds").getAsDouble
        val jsonCommenterObject = jsonCommentObject.getAsJsonObject("commenter")
        val name: String = jsonCommenterObject.getAsJsonPrimitive("name").getAsString
        val jsonMessageObject = jsonCommentObject.getAsJsonObject("message")
        val broadcaster: Boolean = jsonMessageObject.has("broadcaster")
        val moderator: Boolean = jsonMessageObject.has("moderator")
        val subscriber: Boolean = jsonMessageObject.has("subscriber")
        val premium: Boolean = jsonMessageObject.has("premium")
        val message: String = jsonMessageObject.getAsJsonPrimitive("body").getAsString
        val emoticons = new java.util.ArrayList[ChatEmoticon]()
        if (jsonMessageObject.has("emoticons"))
          jsonMessageObject.getAsJsonArray("emoticons").forEach(e => {
            val id = e.getAsJsonObject.getAsJsonPrimitive("_id").getAsString
            val indexBegin = e.getAsJsonObject.getAsJsonPrimitive("begin").getAsInt
            val indexEnd = e.getAsJsonObject.getAsJsonPrimitive("end").getAsInt
            emoticons.add(new TwitchChatEmoticon(message.substring(indexBegin, indexEnd + 1), id, indexBegin))
          })

        val color: String = if (jsonMessageObject.has("user_color")) jsonMessageObject.getAsJsonPrimitive("user_color").getAsString else "#000000"
        messages.add(new TwitchChatMessage(new TwitchChatMessageAuthor(name, broadcaster, moderator, subscriber, premium), message, timestamp, new Channel(channelID), emoticons, color))
      })

      if (jsonObject.has("_next")) {
        val nextPointer = jsonObject.getAsJsonPrimitive("_next").getAsString
        comments = get(s"videos/$videoID/comments", auth = false, oldAPI = true, Seq(("cursor", nextPointer)))
        jsonElement = parser.parse(comments)
        jsonObject = jsonElement.getAsJsonObject
      }
      else hasNext = false
    }

    messages
  }

  /**
    * Shuts down the connector, closes its platform connection.
    */
  override def shutdown(): Unit = {

  }
}

object TwitchAPIConnector {
  val credentialsOauthKey = "oauth"
  val credentialsClientID = "clientid"
}