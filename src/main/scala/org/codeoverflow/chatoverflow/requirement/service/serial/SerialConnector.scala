package org.codeoverflow.chatoverflow.requirement.service.serial

import java.io.{InputStream, PrintStream}

import com.fazecast.jSerialComm.{SerialPort, SerialPortEvent, SerialPortInvalidPortException}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector

import scala.collection.mutable

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
  private val inputListeners: mutable.Map[Array[Byte] => Unit, SerialPortEvent => Unit] = mutable.Map()

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
    val l: SerialPortEvent => Unit = _ => {
      val buffer = new Array[Byte](serialPort.get.bytesAvailable())
      serialPort.get.readBytes(buffer, buffer.length)
      listener(buffer)
    }
    inputListeners += (listener -> l)
    serialPortInputListener.addDataAvailableListener(l)
  }

  def removeInputListener(listener: Array[Byte] => Unit): Unit = {
    inputListeners remove listener match {
      case Some(l) => serialPortInputListener.removeDataAvailableListener(l)
      case _ => //listener not found, do nothing
    }
  }

  /**
    * Opens a connection with the serial port
    */
  override def start(): Boolean = {
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
