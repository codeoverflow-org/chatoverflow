package org.codeoverflow.chatoverflow.requirement.service.tipeeestream.impl

import java.time.OffsetDateTime
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.util.Currency

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.dto.User
import org.codeoverflow.chatoverflow.api.io.dto.stat.stream.tipeeestream._
import org.codeoverflow.chatoverflow.api.io.event.stream.tipeeestream._
import org.codeoverflow.chatoverflow.api.io.input.event.TipeeestreamEventInput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.impl.EventInputImpl
import org.codeoverflow.chatoverflow.requirement.service.tipeeestream.TipeeestreamConnector
import org.codeoverflow.chatoverflow.requirement.service.tipeeestream.TipeeestreamConnector._
import org.json.JSONException

@Impl(impl = classOf[TipeeestreamEventInput], connector = classOf[TipeeestreamConnector])
class TipeeestreamEventInputImpl extends EventInputImpl[TipeeestreamEvent, TipeeestreamConnector] with TipeeestreamEventInput with WithLogger {
  private val DATE_FORMATTER = new DateTimeFormatterBuilder()
    .parseCaseInsensitive().append(DateTimeFormatter.ISO_LOCAL_DATE_TIME).appendOffset("+HHMM", "Z").toFormatter

  override def start(): Boolean = {
    sourceConnector.get.registerEventHandler(onFollow _)
    sourceConnector.get.registerEventHandler(onSubscription _)
    sourceConnector.get.registerEventHandler(onDonation _)
    sourceConnector.get.registerEventHandler(onCheer _)
    true
  }

  private def onDonation(eventJson: DonationEventJSON): Unit = {
    try {
      val event = eventJson.json
      val parameter = event.getJSONObject("parameters")
      val user = new User(parameter.getString("username"))
      val message = parameter.getString("formattedMessage")
      val amount = parameter.getDouble("amount").toFloat
      val currency = Currency.getInstance(parameter.getString("currency"))
      val time = OffsetDateTime.parse(event.getString("created_at"), DATE_FORMATTER)
      val donation = new TipeeestreamDonation(user, amount, currency, time, message)
      call(new TipeeestreamDonationEvent(donation))
    } catch {
      case e: JSONException =>
        logger warn "Error while parsing donation json:"
        logger warn s"${e.getClass.getName} - ${e.getMessage}"
      case e: IllegalArgumentException =>
        logger warn "Error while parsing donation json:"
        logger warn s"${e.getClass.getName} - ${e.getMessage}"
    }
  }

  private def onSubscription(eventJson: SubscriptionEventJSON): Unit = {
    try {
      val event = eventJson.json
      val parameter = event.getJSONObject("parameters")
      val user = new User(parameter.getString("username"))
      val time = OffsetDateTime.parse(event.getString("created_at"), DATE_FORMATTER)
      val resub = parameter.getInt("resub")
      val provider = TipeeestreamProvider.parse(event.getString("origin"))
      val sub = new TipeeestreamSubscription(user, resub, time, provider)
      call(new TipeeestreamSubscriptionEvent(sub))
    } catch {
      case e: JSONException =>
        logger warn "Error while parsing subscription json:"
        logger warn s"${e.getClass.getName} - ${e.getMessage}"
      case e: IllegalArgumentException =>
        logger warn "Error while parsing subscription json:"
        logger warn s"${e.getClass.getName} - ${e.getMessage}"
    }
  }

  private def onFollow(eventJson: FollowEventJSON): Unit = {
    try {
      val event = eventJson.json
      val parameter = event.getJSONObject("parameters")
      val user = new User(parameter.getString("username"))
      val time = OffsetDateTime.parse(event.getString("created_at"), DATE_FORMATTER)
      val provider = TipeeestreamProvider.parse(event.getString("origin"))
      val follow = new TipeeestreamFollow(user, time, provider)
      call(new TipeeestreamFollowEvent(follow))
    } catch {
      case e: JSONException =>
        logger warn "Error while parsing follow json:"
        logger warn s"${e.getClass.getName} - ${e.getMessage}"
      case e: IllegalArgumentException =>
        logger warn "Error while parsing follow json:"
        logger warn s"${e.getClass.getName} - ${e.getMessage}"
    }
  }

  private def onCheer(eventJson: CheerEventJSON): Unit = {
    try {
      val event = eventJson.json
      val parameter = event.getJSONObject("parameters")
      val user = new User(parameter.getString("username"))
      val time = OffsetDateTime.parse(event.getString("created_at"), DATE_FORMATTER)
      val amount = event.getInt("formattedAmount")
      val message = parameter.getString("formattedMessage")
      val cheer = new TipeeestreamCheer(user, amount, message, time)
      call(new TipeeestreamCheerEvent(cheer))
    } catch {
      case e: JSONException =>
        logger warn "Error while parsing follow json:"
        logger warn s"${e.getClass.getName} - ${e.getMessage}"
      case e: IllegalArgumentException =>
        logger warn "Error while parsing follow json:"
        logger warn s"${e.getClass.getName} - ${e.getMessage}"
    }
  }

  override def stop(): Boolean = {
    sourceConnector.get.unregisterAllEventListeners
    true
  }
}
