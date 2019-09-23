package org.codeoverflow.chatoverflow.requirement.service.tipeeestream

import java.util.Calendar

import io.socket.client.{IO, Socket}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.EventConnector
import org.json.JSONObject

/**
  * The tipeeestream connector connects to the socket.io service to work with incoming events.
  *
  * @param sourceIdentifier the name of the tipeeestream account
  */
class TipeeestreamConnector(override val sourceIdentifier: String) extends EventConnector(sourceIdentifier) with WithLogger {
  private val TIMEOUT = 10000
  private val SOCKET_URL = "https://sso-cf.tipeeestream.com"
  private val tipeeeStreamListener = new TipeeestreamListener
  override protected var requiredCredentialKeys: List[String] = List("apiKey", "username")
  override protected var optionalCredentialKeys: List[String] = List()
  private var socket: Option[Socket] = None

  tipeeeStreamListener.registerEventHandler((event, ct) => call(event)(ct))

  override def start(): Boolean = {
    // RestAPI doesn't need startup methods
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
    val url = s"$SOCKET_URL?access_token=${credentials.get.getValue("apiKey").get}"
    socket = Some(IO.socket(url).connect())
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

  override def stop(): Boolean = {
    socket.foreach(_.close())
    true
  }
}

object TipeeestreamConnector {
  private[tipeeestream] sealed class TipeeestreamEventJSON(json: JSONObject)
  private[tipeeestream] case class SubscriptionEventJSON(json: JSONObject) extends TipeeestreamEventJSON(json)
  private[tipeeestream] case class DonationEventJSON(json: JSONObject) extends TipeeestreamEventJSON(json)
  private[tipeeestream] case class FollowEventJSON(json: JSONObject) extends TipeeestreamEventJSON(json)
}