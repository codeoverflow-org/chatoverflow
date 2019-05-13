package org.codeoverflow.chatoverflow.requirement.service.twitter.impl

import java.lang

import com.danielasfregola.twitter4s.entities.Tweet
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.input.twitter._
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.service.twitter
import org.codeoverflow.chatoverflow.requirement.Connection

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

@Impl(impl = classOf[TwitterTweetInput], connector = classOf[twitter.TwitterConnector])
class TwitterConnectorInputImpl extends Connection[twitter.TwitterConnector] with TwitterTweetInput with WithLogger {
  private val timeout: Duration = 5 seconds
  private val tweets: ListBuffer[Tweet] = ListBuffer[Tweet]()

  override def init(): Boolean = {
    sourceConnector.get.init()
  }

  override def serialize(): String = getSourceIdentifier

  override def deserialize(value: String): Unit = {
    setSourceConnector(value)
  }

  override def getTimeLine: String =
    sourceConnector.get.getTimeline(sourceConnector.get.getClient, 5 seconds).get

  override def sendTweet(status: String) =
    sourceConnector.get.sendTweet(sourceConnector.get.getClient, timeout, status)

}