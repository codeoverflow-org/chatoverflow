package org.codeoverflow.chatoverflow.requirement.service.twitter.impl

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.input.twitter._
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.service.twitter
import org.codeoverflow.chatoverflow.requirement.Connection

@Impl(impl = classOf[TwitterTweetInput], connector = classOf[twitter.TwitterConnector])
class TwitterConnectorInputImpl extends Connection[twitter.TwitterConnector] with TwitterTweetInput with WithLogger {
  override def init(): Unit = {
    sourceConnector.get.init()
  }

  override def serialize(): String = getSourceIdentifier

  override def deserialize(value: String): Unit = {
    setSourceConnector(value)
  }
}