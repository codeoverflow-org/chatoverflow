package org.codeoverflow.chatoverflow.requirement.service.serial

import java.io.PrintStream

import com.fazecast.jSerialComm.{SerialPort, SerialPortInvalidPortException}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector

/**
  * The serial connector allows to communicate with a device connected to the pcs serial port (like an Arduino)
  *
  * @param sourceIdentifier the port descriptor of the serial port to which the device is connected
  */
class SerialConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {

  override protected var requiredCredentialKeys: List[String] = List()

  private var serialPort: Option[SerialPort] = None
  private var out: Option[PrintStream] = None
  private val serialPortInputListener = new SerialPortInputListener

  /**
    * Sets the baud rate of the com port to a new value
    *
    * @param baudRate the new baud rate
    * @throws java.lang.IllegalStateException if the serial port is not available yet
    */
  @throws(classOf[IllegalStateException])
  def setBaudRate(baudRate: Int): Unit = {
    if (serialPort.isEmpty) throw new IllegalStateException("Serial port is not available yet")
    serialPort.get.setBaudRate(baudRate)
  }

  /**
    *
    * @throws java.lang.IllegalStateException if the serial port is not available yet
    * @return the baud rate of the com port
    */
  @throws(classOf[IllegalStateException])
  def getBaudRate: Int = {
    if (serialPort.isEmpty) throw new IllegalStateException("Serial port is not available yet")
    serialPort.get.getBaudRate
  }

  /**
    *
    * @throws java.lang.IllegalStateException if the serial port is not available yet
    * @return print stream that outputs to the port
    */
  @throws(classOf[IllegalStateException])
  def getPrintStream: PrintStream = {
    if (serialPort.isEmpty)  throw new IllegalStateException("Serial port is not available yet")
    out.get
  }

  /**
    * Adds a new input listener that receives all data
    * @param listener a listener that handles incoming data in a byte array
    */
  def addInputListener(listener: Array[Byte] => Unit): Unit = {
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
    try {
      serialPort = Some(SerialPort.getCommPort(sourceIdentifier))
      if (serialPort.get.openPort(1000)) {
        serialPort.get.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0)
        out = Some(new PrintStream(serialPort.get.getOutputStream))
        serialPort.get.addDataListener(serialPortInputListener)
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
