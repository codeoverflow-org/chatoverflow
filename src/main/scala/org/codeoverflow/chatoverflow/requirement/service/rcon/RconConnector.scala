package org.codeoverflow.chatoverflow.requirement.service.rcon

import java.io.{DataInputStream, IOException, InputStream, OutputStream}
import java.net.{Socket, SocketException}
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
    if (write(2, command.getBytes("ASCII"))) {
      return read()
    }
    null
  }


  /**
    * Starts the connector, e.g. creates a connection with its platform.
    */
  override def start(): Boolean = {
    logger info s"Starting rcon connection to ${credentials.get.getValue("address").get}"
    var port: Int = 25575
    if (credentials.get.exists("port")) {
      try{
        port = credentials.get.getValue("port").get.toInt
      } catch {
        case e: NumberFormatException => {
          logger error "Please enter a valid port"
          return false
        }
      }
      if (port < 1 || port > 65535) {
        logger error "Please enter a valid port"
        return false
      }
    }
    try {
      socket = new Socket(credentials.get.getValue("address").get, port)
      socket.setKeepAlive(true)
      outputStream = socket.getOutputStream
      inputStream = socket.getInputStream
    } catch {
      case e: IOException => {
        logger error "No Connection to RCON Server. Is it up?"
        return false
      }
    }
    val loggedIn = login()
    // Sleeping here to allow the (minecraft) server to start its own rcon procedure. Otherwise it caused errors in my tests.
    Thread.sleep(5000)
    loggedIn
  }

  private def login(): Boolean = {
    requestId = new Random().nextInt(Integer.MAX_VALUE)
    logger info "Logging RCON in..."
    val password = credentials.get.getValue("password").get
    if (write(3, password.getBytes("ASCII"))) {
      if (read() == null) {
        logger error "Could not log in to RCON Server. Password is Wrong!"
        return false
      } else {
        logger debug "Login to RCON was successful"
        return true
      }
    }
    false
  }

  private def write(packageType: Int, payload: Array[Byte]): Boolean = {
    try  {
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
    } catch {
      case e: SocketException => {
        logger error "Connection Error to RCON Server. This request will not be sended!"
        return false
      }
    }
    true
  }

  private def read(): String = {
    try {
      val header: Array[Byte] = Array.ofDim[Byte](4*3)
      inputStream.read(header)
      val headerBuffer: ByteBuffer = ByteBuffer.wrap(header)
      headerBuffer.order(ByteOrder.LITTLE_ENDIAN)
      val length = headerBuffer.getInt()
      val packageType = headerBuffer.getInt
      val payload: Array[Byte] = Array.ofDim[Byte](length - 4 - 4 - 2)
      val dataInputStream: DataInputStream = new DataInputStream(inputStream)
      dataInputStream.readFully(payload)
      dataInputStream.read(Array.ofDim[Byte](2))
      if (packageType == -1) {
        return null
      }
      new String(payload, "ASCII")
    } catch {
      case e: NegativeArraySizeException => null;
    }
  }

  /**
    * This stops the activity of the connector, e.g. by closing the platform connection.
    */
  override def stop(): Boolean = {
    logger info s"Stopped RCON connector to ${credentials.get.getValue("address").get}!"
    socket.close()
    true
  }
}
