package org.codeoverflow.chatoverflow.requirement.service.tipeeestream.impl

import java.time.OffsetDateTime
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.util.Currency

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.dto.User
import org.codeoverflow.chatoverflow.api.io.dto.stat.stream.tipeeestream.{TipeeestreamDonation, TipeeestreamFollow, TipeeestreamProvider, TipeeestreamSubscription}
import org.codeoverflow.chatoverflow.api.io.event.stream.tipeeestream.{TipeeestramFollowEvent, TipeeestreamDonationEvent, TipeeestreamEvent, TipeeestreamSubscriptionEvent}
import org.codeoverflow.chatoverflow.api.io.input.event.TipeeestreamEventInput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.impl.EventInputImpl
import org.codeoverflow.chatoverflow.requirement.service.tipeeestream.TipeeestreamConnector
import org.json.{JSONException, JSONObject}

@Impl(impl = classOf[TipeeestreamEventInput], connector = classOf[TipeeestreamConnector])
class TipeestreamEventInputImpl extends EventInputImpl[TipeeestreamEvent, TipeeestreamConnector] with TipeeestreamEventInput with WithLogger {
  private val DATE_FORMATTER = new DateTimeFormatterBuilder()
    .parseCaseInsensitive().append(DateTimeFormatter.ISO_LOCAL_DATE_TIME).appendOffset("+HHMM", "Z").toFormatter

  private val onFollowFn = onFollow _
  private val onSubscriptionFn = onSubscription _
  private val onDonationFn = onDonation _

  override def start(): Boolean = {
    sourceConnector.get.addFollowEventListener(onFollowFn)
    sourceConnector.get.addSubscriptionEventListener(onSubscriptionFn)
    sourceConnector.get.addDonationEventListener(onDonationFn)
    true
  }

  private def onDonation(event: JSONObject): Unit = {
    try {
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

  private def onSubscription(event: JSONObject): Unit = {
    try {
      val parameter = event.getJSONObject("parameters")
      val user = new User(parameter.getString("username"))
      val time = OffsetDateTime.parse(event.getString("created_at"), DATE_FORMATTER)
      val resub = parameter.getInt("resub")
      val provider = TipeeestreamProvider.parse(event.getString("origin"))
      val sub = new TipeeestreamSubscription(user, resub, time, provider)
      call(new TipeeestreamSubscriptionEvent(sub))
    } catch {
      case e: JSONException =>
        logger warn "Error while parsing donation json:"
        logger warn s"${e.getClass.getName} - ${e.getMessage}"
      case e: IllegalArgumentException =>
        logger warn "Error while parsing donation json:"
        logger warn s"${e.getClass.getName} - ${e.getMessage}"
    }
  }

  private def onFollow(event: JSONObject): Unit = {
    try {
      val parameter = event.getJSONObject("parameters")
      val user = new User(parameter.getString("username"))
      val time = OffsetDateTime.parse(event.getString("created_at"), DATE_FORMATTER)
      val provider = TipeeestreamProvider.parse(event.getString("origin"))
      val follow = new TipeeestreamFollow(user, time, provider)
      call(new TipeeestramFollowEvent(follow))
    } catch {
      case e: JSONException =>
        logger warn "Error while parsing donation json:"
        logger warn s"${e.getClass.getName} - ${e.getMessage}"
      case e: IllegalArgumentException =>
        logger warn "Error while parsing donation json:"
        logger warn s"${e.getClass.getName} - ${e.getMessage}"
    }
  }

  override def stop(): Boolean = {
    sourceConnector.get.removeFollowEventListener(onFollowFn)
    sourceConnector.get.removeSubscriptionEventListener(onSubscriptionFn)
    sourceConnector.get.removeDonationEventListener(onDonationFn)
    true
  }
}
