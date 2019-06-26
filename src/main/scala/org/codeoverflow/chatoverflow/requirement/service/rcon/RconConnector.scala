package org.codeoverflow.chatoverflow.requirement.service.rcon

import java.io.{InputStream, OutputStream}
import java.net.Socket
import java.nio.{ByteBuffer, ByteOrder}
import java.util.Random

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector

class RconConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {
  override protected var requiredCredentialKeys: List[String] = List("password", "address")
  override protected var optionalCredentialKeys: List[String] = List("port")

  private var socket: Socket = _
  private var outputStream: OutputStream = _
  private var inputStream: InputStream = _
  private var requestId: Int = 0

  def sendCommand(command: String): String = {
    logger debug s"Sending $command to RCON"
    requestId += 1
    write(2, command.getBytes("ASCII"))
    ""
  }


  /**
    * Starts the connector, e.g. creates a connection with its platform.
    */
  override def start(): Boolean = {
    logger info s"Starting rcon connection to ${credentials.get.getValue("address").get}"
    var port: Int = 25575
    if (credentials.get.exists("port")) {
      port = credentials.get.getValue("port").get.toInt
      if (port < 1 || port > 65535) {
        return false
      }
    }
    socket = new Socket(credentials.get.getValue("address").get, port)
    outputStream = socket.getOutputStream
    inputStream = socket.getInputStream
    login()
    true
  }

  private def login(): Unit = {
    requestId = new Random().nextInt(Integer.MAX_VALUE)
    logger info "Logging RCON in..."
    val password = credentials.get.getValue("password").get
    write(3, password.getBytes("ASCII"))
    logger debug "RCON Login sent"
  }

  private def write(packageType: Int, payload: Array[Byte]): Boolean = {
    val length = 4 + 4 + payload.length + 1 + 1
    var byteBuffer: ByteBuffer = ByteBuffer.allocate(length + 4)
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

    byteBuffer.putInt(length)
    byteBuffer.putInt(requestId)
    byteBuffer.putInt(packageType)
    byteBuffer.put(payload)
    byteBuffer.put(0x00.toByte)
    byteBuffer.put(0x00.toByte)

    outputStream.write(byteBuffer.array())
    outputStream.flush()
    true
  }

  /**
    * This stops the activity of the connector, e.g. by closing the platform connection.
    */
  override def stop(): Boolean = {
    logger info s"Stopped RCON connector to ${credentials.get.getValue("address")}!"
    socket.close()
    true
  }
}
