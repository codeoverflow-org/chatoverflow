package org.codeoverflow.chatoverflow.requirement.service.serial.impl

import java.io.PrintStream

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.output.SerialOutput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.OutputImpl
import org.codeoverflow.chatoverflow.requirement.service.serial.SerialConnector

@Impl(impl = classOf[SerialOutput], connector = classOf[SerialConnector])
class SerialOutputImpl extends OutputImpl[SerialConnector] with SerialOutput with WithLogger {

  override def start(): Boolean = true

  override def getPrintStream: PrintStream = sourceConnector.get.getPrintStream

  /**
    * Stops the output, called before source connector will shutdown
    *
    * @return true if stopping was successful
    */
  override def stop(): Boolean = true
}
