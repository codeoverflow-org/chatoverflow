package org.codeoverflow.chatoverflow.requirement.service.tipeeestream

import java.io.IOException
import java.time.OffsetDateTime
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.util.Calendar

import io.socket.client.{IO, Socket}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector
import org.codeoverflow.chatoverflow.connector.actor.HttpActor
import org.codeoverflow.chatoverflow.connector.actor.HttpActor.GetRequest
import org.json.JSONObject
import org.json4s.jackson.JsonMethods._
import org.json4s._

import scala.annotation.tailrec

/**
  * The tipeeestream connector connects to the socket.io service to work with incoming events.
  *
  * @param sourceIdentifier the name of the tipeeestream account
  */
class TipeeestreamConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {
  private val TIMEOUT = 10000
  private val DATE_FORMATTER = new DateTimeFormatterBuilder()
    .parseCaseInsensitive().append(DateTimeFormatter.ISO_LOCAL_DATE_TIME).appendOffset("+HHMM", "Z").toFormatter
  private val SOCKET_URL = "https://sso-cf.tipeeestream.com"
  private val REST_BASE_URL = "https://api.tipeeestream.com/v1.0"
  private val tipeeeStreamListener = new TipeeestreamListener
  private val httpActor = createActor[HttpActor]()
  override protected var requiredCredentialKeys: List[String] = List("apiKey", "username")
  override protected var optionalCredentialKeys: List[String] = List()
  private var socket: Option[Socket] = None

  override def start(): Boolean = {
    //RestAPI doesn't need startup methods
    startSocket()
  }

  /**
    * Start the socket.io socket
    *
    * @return if the socket could start successfully
    */
  private def startSocket(): Boolean = {
    @volatile var connected: Option[Boolean] = None
    val thread = Thread.currentThread
    socket = Some(IO.socket(SOCKET_URL).connect())
    socket.get.on(Socket.EVENT_CONNECT, (_: Any) => {
      logger info "Connected to TipeeeStream Socket.io"
      socket.get.emit("join-room", AUTH_OBJECT)
      logger info "emitted credentials to TipeeeStream Socket.io api"
      socket.get.on("new-event", (objects: Array[AnyRef]) => {
        tipeeeStreamListener.onSocketEvent(objects)
      })
      connected = Some(true)
    })
    socket.get.on(Socket.EVENT_CONNECT_ERROR, (e: Any) => {
      logger warn s"Could not connect to TipeeeStream socket:"
      e match {
        case array: Array[Any] => logger warn array.mkString(", ")
        case other => logger warn other.toString
      }
      connected = Some(false)
    })
    socket.get.on(Socket.EVENT_CONNECT_TIMEOUT, (_: Any) => {
      logger warn s"$sourceIdentifier socket timed out"
    })
    socket.get.on(Socket.EVENT_ERROR, (e: Any) => {
      logger warn s"$sourceIdentifier socket error:"
      e match {
        case array: Array[Any] => logger warn array.mkString(", ")
        case other => logger warn other.toString
      }
    })
    val start = Calendar.getInstance.getTimeInMillis
    try {
      while (connected.isEmpty && start + TIMEOUT > Calendar.getInstance.getTimeInMillis) Thread.sleep(100)
    } catch {
      case _: InterruptedException => //Just resume
    }
    connected.getOrElse({
      logger warn "Could not connect to TipeeeStream socket: Timed out!"
      false
    })
  }

  private def AUTH_OBJECT: JSONObject = {
    val obj = new JSONObject()
    obj.put("room", credentials.get.getValue("apiKey").get)
    obj.put("username", credentials.get.getValue("username").get)
    obj
  }

  def addSubscriptionEventListener(listener: JSONObject => Unit): Unit = tipeeeStreamListener.addSubscriptionEventListener(listener)

  def addDonationEventListener(listener: JSONObject => Unit): Unit = tipeeeStreamListener.addDonationEventListener(listener)

  def addFollowEventListener(listener: JSONObject => Unit): Unit = tipeeeStreamListener.addFollowEventListener(listener)

  /**
    * Request your account information (username, avatar, connected services,...) from Tipeeestream REST API
    *
    * @return Json object containing all your event statistics
    */
  def getAccountInfo: JValue = {
    httpActor.??[String](2)(GetRequest(
      s"$REST_BASE_URL/me.json",
      queryParams = Seq(("apiKey", credentials.get.getValue("apiKey").get))
    )) match {
      case Some(response) => parse(response)
      case _ => throw new IOException("Couldn't request account info from Tipeeestream REST API")
    }
  }

  /**
    * Request your event statistics (subs, donations, follows,...) from Tipeeestream REST API
    *
    * @return Json object containing all your event statistics
    */
  def getEventStats: JValue = {
    httpActor.??[String](2)(GetRequest(
      s"$REST_BASE_URL/events/forever.json",
      queryParams = Seq(("apiKey", credentials.get.getValue("apiKey").get))
    )) match {
      case Some(response) => parse(response)
      case _ => throw new IOException("Couldn't request stats from TipeeeStream REST API")
    }
  }

  /**
    * Request detailed information about all events (subs, donations, follows,...) since the given start time from Tipeeestream REST API
    *
    * May need multiple requests if more than 25 events are requested and therefore returns a sequence of all responses
    *
    * @param types types of the events that should be requested
    * @param start start time
    * @return a list of responses (each contains info for up to 25 events)
    */
  def getRecentEvents(types: Seq[String], start: OffsetDateTime): Seq[JValue] = {
    implicit val formats: Formats = DefaultFormats

    @tailrec
    def getRecentEventsWithOffset(types: Seq[String], start: OffsetDateTime, offset: Int, previous: Seq[JValue]): Seq[JValue] = {
      httpActor.??[String](2)(GetRequest(
        s"$REST_BASE_URL/events.json",
        queryParams = ("apiKey", credentials.get.getValue("apiKey").get)
          +: types.map(t => ("type[]", t))
          :+ ("start", start.format(DATE_FORMATTER))
          :+ ("limit", "25")
          :+ ("offset", offset.toString)
      )) match {
        case Some(r) =>
          val response = parse(r)
          val total = (response \ "datas" \ "total_count").extract[Int]
          if (total > 25 * (offset + 1)) {
            getRecentEventsWithOffset(types, start, offset + 1, previous :+ response)
          } else {
            previous :+ response
          }
        case _ => throw new IOException("Couldn't request events from TipeeeStream REST API")
      }
    }

    getRecentEventsWithOffset(types, start, 0, Seq())
  }

  override def stop(): Boolean = {
    socket.foreach(_.close())
    true
  }
}
