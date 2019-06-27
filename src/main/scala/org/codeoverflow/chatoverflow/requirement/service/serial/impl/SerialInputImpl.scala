package org.codeoverflow.chatoverflow.requirement.service.serial.impl

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.input.SerialInput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.InputImpl
import org.codeoverflow.chatoverflow.requirement.service.serial.SerialConnector

import scala.collection.mutable.ListBuffer

@Impl(impl = classOf[SerialInput], connector = classOf[SerialConnector])
class SerialInputImpl extends InputImpl[SerialConnector] with SerialInput with WithLogger {

  private val stringListeners = ListBuffer[Consumer[String]]()
  private val byteListeners = ListBuffer[Consumer[Array[Byte]]]()

  override def start(): Boolean = {
    sourceConnector.get.addInputListener(onIncomingData)
    true
  }

  private def onIncomingData(bytes: Array[Byte]): Unit = {
    byteListeners.foreach(l => l.accept(bytes))
    stringListeners.foreach(l => l.accept(new String(bytes, StandardCharsets.US_ASCII)))
  }

  override def registerStringListener(consumer: Consumer[String]): Unit = stringListeners += consumer

  override def registerDataListener(consumer: Consumer[Array[Byte]]): Unit = byteListeners += consumer

  override def getInputStream: InputStream = sourceConnector.get.getInputStream

  /**
    * Stops the input, called before source connector will shutdown
    *
    * @return true if stopping was successful
    */
  override def stop(): Boolean = true
}
