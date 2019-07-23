package org.codeoverflow.chatoverflow.requirement.service.twitter.impl


import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.output.twitter.TwitterTweetOutput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.impl.OutputImpl
import org.codeoverflow.chatoverflow.requirement.service.twitter.TwitterConnector

@Impl(impl = classOf[TwitterTweetOutput], connector = classOf[TwitterConnector])
class TwitterConnectorOutputImpl extends OutputImpl[TwitterConnector] with TwitterTweetOutput with WithLogger {

  override def init(): Boolean = {
    sourceConnector.get.init()
  }

  override def serialize(): String = getSourceIdentifier

  override def deserialize(value: String): Unit = {
    setSourceConnector(value)
  }

  override def start() = true

  /**
    * Stops the output, called before source connector will shutdown
    *
    * @return true if stopping was successful
    */
  override def stop(): Boolean = true

  override def sendTweet(status: String): Boolean = {
    sourceConnector.get.sendTweet(status)
  }
}