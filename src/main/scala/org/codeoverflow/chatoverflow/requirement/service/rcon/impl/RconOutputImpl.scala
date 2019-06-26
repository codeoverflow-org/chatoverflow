package org.codeoverflow.chatoverflow.requirement.service.rcon.impl

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.output.RconOutput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.OutputImpl
import org.codeoverflow.chatoverflow.requirement.service.rcon.RconConnector

@Impl(impl = classOf[RconOutput], connector = classOf[RconConnector])
class RconOutputImpl extends OutputImpl[RconConnector] with RconOutput with WithLogger {
  override def sendCommand(command: String): String = sourceConnector.get.sendCommand(command)

  /**
    * Start the input, called after source connector did init
    *
    * @return true if starting the input was successful, false if some problems occurred
    */
  override def start(): Boolean = true
}
