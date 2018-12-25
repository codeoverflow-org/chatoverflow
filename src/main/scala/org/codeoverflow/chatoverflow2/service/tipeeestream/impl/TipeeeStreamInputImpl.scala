package org.codeoverflow.chatoverflow2.service.tipeeestream.impl

import org.codeoverflow.chatoverflow.api.io.input.event.SubscriptionEventInput
import org.codeoverflow.chatoverflow2.requirement.Connection
import org.codeoverflow.chatoverflow2.service.tipeeestream.TipeeeStreamConnector

// TODO: This class should have probably a counterpart in the API. Not now, for testing only
class TipeeeStreamInputImpl extends Connection[TipeeeStreamConnector] with SubscriptionEventInput {
  override def init(): Unit = ???

  override def serialize(): String = ???

  override def deserialize(value: String): Unit = ???
}
