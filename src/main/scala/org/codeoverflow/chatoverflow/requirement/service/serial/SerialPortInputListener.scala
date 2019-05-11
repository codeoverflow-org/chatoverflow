package org.codeoverflow.chatoverflow.requirement.service.serial

import com.fazecast.jSerialComm.{SerialPort, SerialPortDataListener, SerialPortEvent}

import scala.collection.mutable.ListBuffer

class SerialPortInputListener extends SerialPortDataListener {

  private val listeners = ListBuffer[SerialPortEvent => Unit]()

  override def getListeningEvents: Int = SerialPort.LISTENING_EVENT_DATA_AVAILABLE

  override def serialEvent(event: SerialPortEvent): Unit = {
    if (event.getEventType == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
      listeners.foreach(listener => listener(event))
    }
  }

  def addDataAvailableListener(listener: SerialPortEvent => Unit): Unit = listeners += listener
}
