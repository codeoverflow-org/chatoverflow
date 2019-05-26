package org.codeoverflow.chatoverflow.requirement.service.tipeeestream.impl

import java.util.function.Consumer

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.dto.event.tipeeestream.{TipeeeStreamDonation, TipeeeStreamEvent, TipeeeStreamFollow, TipeeeStreamSubscription}
import org.codeoverflow.chatoverflow.api.io.input.chat.TipeeeStreamInput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.InputImpl
import org.codeoverflow.chatoverflow.requirement.service.tipeeestream.TipeeeStreamConnector

import scala.collection.mutable.ListBuffer

@Impl(impl = classOf[TipeeeStreamInput], connector = classOf[TipeeeStreamConnector])
class TipeeeStreamInputImpl extends InputImpl[TipeeeStreamConnector] with TipeeeStreamInput with WithLogger {
  private val donationHandler = ListBuffer[Consumer[TipeeeStreamDonation]]()
  private val subscriptionHandler = ListBuffer[Consumer[TipeeeStreamSubscription]]()
  private val followHandler = ListBuffer[Consumer[TipeeeStreamFollow]]()

  def onEvent[T <: TipeeeStreamEvent] (event: T ): Unit = {
    event match {
      case event: TipeeeStreamDonation => donationHandler.foreach(_.accept(event.asInstanceOf[TipeeeStreamDonation]))
      case event: TipeeeStreamFollow => followHandler.foreach(_.accept(event.asInstanceOf[TipeeeStreamFollow]))
      case event: TipeeeStreamSubscription => subscriptionHandler.foreach(_.accept(event.asInstanceOf[TipeeeStreamSubscription]))
    }
  }

  override def start(): Boolean = {
    sourceConnector.get.addIncomingEventHandler(onEvent)
    true
  }

  /**
    * Let's you register a simple handler immediately react on new subscriptions
    *
    * @param handler the consumer t ehandle incoming messages
    */
  override def registerSubscriptionHandler(handler: Consumer[TipeeeStreamSubscription]): Unit = {
    subscriptionHandler += handler
  }

  override def registerDonationHandler(handler: Consumer[TipeeeStreamDonation]): Unit = {
    donationHandler += handler
  }

  override def registerFollowHandler(handler: Consumer[TipeeeStreamFollow]): Unit = {
    followHandler += handler
  }
}
