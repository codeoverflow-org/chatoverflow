package org.codeoverflow.chatoverflow.requirement.service.rcon.impl

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.input.RconInput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.impl.InputImpl
import org.codeoverflow.chatoverflow.requirement.service.rcon.RconConnector

@Impl(impl = classOf[RconInput], connector = classOf[RconConnector])
class RconInputImpl extends InputImpl[RconConnector] with RconInput with WithLogger {
  override def getCommandOutput(command: String): String = sourceConnector.get.sendCommand(command)

  /**
    * Start the input, called after source connector did init
    *
    * @return true if starting the input was successful, false if some problems occurred
    */
  override def start(): Boolean = sourceConnector.get.isLoggedIn

  override def stop(): Boolean = true
}
