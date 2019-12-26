package org.codeoverflow.chatoverflow.requirement.service.hosting.impl

import java.io.File

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.output.HostingOutput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.impl.OutputImpl
import org.codeoverflow.chatoverflow.requirement.service.hosting.HostingConnector

@Impl(impl = classOf[HostingOutput], connector = classOf[HostingConnector])
class HostingOutputImpl extends OutputImpl[HostingConnector] with HostingOutput with WithLogger {
  /**
    * Start the input, called after source connector did init
    *
    * @return true if starting the input was successful, false if some problems occurred
    */
  override def start(): Boolean = true

  /**
    * Stops the output, called before source connector will shutdown
    *
    * @return true if stopping was successful
    */
  override def stop(): Boolean = true

  override def host(file: File): Boolean = {
    val endpoint = file.getName
    host(file, endpoint)
  }

  override def host(file: File, endpoint: String): Boolean = {
    sourceConnector.get.addHostedEntity(file, endpoint)
  }
}
