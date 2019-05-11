package org.codeoverflow.chatoverflow.requirement.service.sample.impl

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.input.SampleInput
import org.codeoverflow.chatoverflow.requirement.Connection
import org.codeoverflow.chatoverflow.requirement.service.sample.SampleConnector

class SampleInputImpl extends Connection[SampleConnector] with SampleInput with WithLogger {
  override def init(): Boolean = sourceConnector.get.init()

  override def serialize(): String = getSourceIdentifier

  override def deserialize(value: String): Unit = setSourceConnector(value)
}
