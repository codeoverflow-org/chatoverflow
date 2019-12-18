package org.codeoverflow.chatoverflow.requirement.service.twitter.impl

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.input.twitter.TwitterSearchInput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.impl.InputImpl
import org.codeoverflow.chatoverflow.requirement.service.twitter.TwitterConnector


/**
 * This is the implementation of the twitter input, using the twitter connector.
 */
@Impl(impl = classOf[TwitterSearchInput], connector = classOf[TwitterConnector])
class TwitterConnectorInputImpl extends InputImpl[TwitterConnector] with TwitterSearchInput with WithLogger {
  var since_id: Long = _

  /**
   * Starts the input, called before source connector will shutdown
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

  override def searchTweet(query: String): Boolean = {
    try {
      val result = sourceConnector.get.searchTweet(query, since_id = Option(since_id))
      if (result._1) {
        logger info s"Successfully fetched " + result._3.get.length + " Tweets"
        for (tweet <- result._3.get) {
          logger info s"${tweet.user.get.name} (@${tweet.user.get.screen_name}): ${tweet.text} at ${tweet.created_at}"
          logger info tweet.id
          since_id = tweet.id
        }
        true
      } else {
        logger error s"Error searching tweet: ${result._2}"
        false
      }
    }
    catch {
      case e: Exception => {
        logger error e
        false
      }
    }
  }

}