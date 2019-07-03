package org.codeoverflow.chatoverflow.requirement.service.sample.impl

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.input.SampleInput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.impl.InputImpl
import org.codeoverflow.chatoverflow.requirement.service.sample.SampleConnector

@Impl(impl = classOf[SampleInput], connector = classOf[SampleConnector])
class SampleInputImpl extends InputImpl[SampleConnector] with SampleInput with WithLogger {

  override def start(): Boolean = true

  /**
    * Stops the input, called before source connector will shutdown
    *
    * @return true if stopping was successful
    */
  override def stop(): Boolean = true
}
