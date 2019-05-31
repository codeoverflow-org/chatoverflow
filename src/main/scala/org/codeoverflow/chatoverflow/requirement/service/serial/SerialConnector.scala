package org.codeoverflow.chatoverflow.requirement.service.serial

import java.io.{InputStream, PrintStream}

import com.fazecast.jSerialComm.{SerialPort, SerialPortInvalidPortException}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector

/**
  * The serial connector allows to communicate with a device connected to the pcs serial port (like an Arduino)
  *
  * @param sourceIdentifier r the unique source identifier to identify this connector
  */
class SerialConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {

  private val serialPortInputListener = new SerialPortInputListener
  override protected var optionalCredentialKeys: List[String] = List("baudRate")
  override protected var requiredCredentialKeys: List[String] = List("port")
  private var serialPort: Option[SerialPort] = None
  private var out: Option[PrintStream] = None
  private var in: Option[InputStream] = None

  /**
    * @throws java.lang.IllegalStateException if the serial port is not available yet
    * @return print stream that outputs to the port
    */
  @throws(classOf[IllegalStateException])
  def getPrintStream: PrintStream = {
    if (serialPort.isEmpty) throw new IllegalStateException("Serial port is not available yet")
    out.get
  }

  /**
    * @throws java.lang.IllegalStateException if the serial port is not available yet
    * @return a inputstream that receives all data from the port
    */
  @throws(classOf[IllegalStateException])
  def getInputStream: InputStream = {
    if (serialPort.isEmpty) throw new IllegalStateException("Serial port is not available yet")
    in.get
  }

  /**
    * Adds a new input listener that receives all data
    *
    * @param listener a listener that handles incoming data in a byte array
    * @throws java.lang.IllegalStateException if the serial port is not available yet
    */
  @throws(classOf[IllegalStateException])
  def addInputListener(listener: Array[Byte] => Unit): Unit = {
    if (serialPort.isEmpty) throw new IllegalStateException("Serial port is not available yet")
    serialPortInputListener.addDataAvailableListener(_ => {
      val buffer = new Array[Byte](serialPort.get.bytesAvailable())
      serialPort.get.readBytes(buffer, buffer.length) //FIXME DOES IT CRASH?
      listener(buffer)
    })
  }

  /**
    * Opens a connection with the serial port
    */
  override def start(): Boolean = {
    //TODO Test if connector is working this way or if it requires an actor
    try {
      serialPort = Some(SerialPort.getCommPort(credentials.get.getValue("port").get))
      credentials.get.getValue("baudRate") match {
        case Some(baudRate) if baudRate.matches("\\s*\\d+\\s*") => serialPort.get.setBaudRate(baudRate.trim.toInt)
        case Some(ivalidBaudrate) =>
          logger error s"Invalid baud rate: $ivalidBaudrate"
          return false
        case None => //Do nothing
      }
      logger info s"Waiting for serial port to open..."
      if (serialPort.get.openPort(1000)) {
        Thread.sleep(1500) //Sleep to wait for
        serialPort.get.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0)
        out = Some(new PrintStream(serialPort.get.getOutputStream, true, "US-ASCII"))
        in = Some(serialPort.get.getInputStream)
        serialPort.get.addDataListener(serialPortInputListener)
        logger info "Opened serial port!"
        true
      } else {
        logger error s"Could not open serial port $sourceIdentifier"
        false
      }
    } catch {
      case e: SerialPortInvalidPortException =>
        logger error s"Source identifier $sourceIdentifier is invalid: ${e.getMessage}"
        false
    }
  }

  /**
    * Closes the connection with the port
    */
  override def stop(): Boolean = {
    serialPort.foreach(_.closePort())
    true
  }
}
