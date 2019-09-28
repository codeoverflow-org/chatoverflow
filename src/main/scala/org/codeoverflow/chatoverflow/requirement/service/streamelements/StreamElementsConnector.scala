package org.codeoverflow.chatoverflow.requirement.service.streamelements

import io.socket.client.Socket._
import io.socket.client.{IO, Socket}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.EventConnector
import org.json.JSONObject

class StreamElementsConnector(sourceIdentifier: String) extends EventConnector(sourceIdentifier) with WithLogger {
  override protected var requiredCredentialKeys: List[String] = List("jwt-token")
  override protected var optionalCredentialKeys: List[String] = List()

  private val TIMEOUT = 10000
  private val SOCKET_URL = "https://realtime.streamelements.com"
  private var socket: Option[Socket] = None
  private var connected: Option[Boolean] = None
  private val listener = new StreamElementsListener
  listener.registerEventHandler((e, ct) => call(e)(ct)) // pass events to in/output.

  override def start(): Boolean = {
    logger info "Connecting to the StreamElements websocket..."

    val opts = new IO.Options()
    opts.transports = Array("websocket")

    socket = Some(IO.socket(SOCKET_URL, opts).connect())
    registerSocketEvents(socket.get)

    connected.synchronized {
      connected.wait(TIMEOUT)
    }

    connected.getOrElse({
      logger warn "Could not connect to StreamElements socket: Timed out!"
      false
    })
  }

  private def registerSocketEvents(s: Socket): Unit = {
    def setConnected(isConnected: Boolean): Unit = connected.synchronized {
      connected.notify()
      connected = Some(isConnected)
    }

    s.on(EVENT_CONNECT, (_: Any) => {
      logger info "Successfully connected to the StreamElements websocket."

      val authObj = new JSONObject()
        .put("method", "jwt")
        .put("token", credentials.get.getValue("jwt-token").get)

      logger info "Authenticating with the StreamElements websocket..."
      s.emit("authenticate", authObj)
    })

    s.on(EVENT_CONNECT_ERROR, (e: Array[AnyRef]) => {
      logger warn s"Could not connect to StreamElements socket:"
      logger warn e.mkString(", ")

      setConnected(false)
    })

    s.on(EVENT_CONNECT_TIMEOUT, (_: Any) => {
      setConnected(false)
    })

    s.on(EVENT_ERROR, (e: Array[AnyRef]) => {
      logger warn s"StreamElements($sourceIdentifier) socket error:"
      logger warn e.mkString(", ")
    })

    s.on(EVENT_DISCONNECT, (_: Any) => {
      logger info "Disconnected from the StreamElements websocket."
    })

    s.on("authenticated", (_: Any) => {
      logger info "Successfully authenticated to the StreamElements websocket."

      setConnected(true)
    })

    s.on("event", (event: Array[AnyRef]) => listener.handleEvent(event))
  }

  override def stop(): Boolean = {
    if (socket.isDefined) {
      socket.get.close()
    }
    connected = None
    socket = None
    true
  }
}

object StreamElementsConnector {
    private[streamelements] sealed class StreamElementsEventJSON(json: JSONObject)
    private[streamelements] case class SubscriptionEventJSON(json: JSONObject) extends StreamElementsEventJSON(json)
    private[streamelements] case class DonationEventJSON(json: JSONObject) extends StreamElementsEventJSON(json)
    private[streamelements] case class FollowEventJSON(json: JSONObject) extends StreamElementsEventJSON(json)
    private[streamelements] case class CheerEventJSON(json: JSONObject) extends StreamElementsEventJSON(json)
    private[streamelements] case class HostEventJSON(json: JSONObject) extends StreamElementsEventJSON(json)
    private[streamelements] case class RaidEventJSON(json: JSONObject) extends StreamElementsEventJSON(json)
}
