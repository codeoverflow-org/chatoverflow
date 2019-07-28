package org.codeoverflow.chatoverflow.requirement.service.twitter.impl


import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.output.twitter.TwitterTweetOutput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.impl.OutputImpl
import org.codeoverflow.chatoverflow.requirement.service.twitter.TwitterConnector


/**
  * This is the implementation of the twitter output, using the twitter connector.
  */
@Impl(impl = classOf[TwitterTweetOutput], connector = classOf[TwitterConnector])
class TwitterConnectorOutputImpl extends OutputImpl[TwitterConnector] with TwitterTweetOutput with WithLogger {
  /**
    * Starts the output, called before source connector will shutdown
    *
    * @return true if stopping was successful
    */
  override def start() = true

  /**
    * Stops the output, called before source connector will shutdown
    *
    * @return true if stopping was successful
    */
  override def stop() = true

  override def sendTweet(status: String): Boolean = {
    val result = sourceConnector.get.sendTextTweet(status)
    if (result._1) {
      logger info s"Successfully send tweet: $status"
      true
    } else {
      logger error s"Error sending tweet: ${result._2}"
      false
    }
  }
}