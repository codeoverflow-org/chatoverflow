package org.codeoverflow.chatoverflow.requirement.service.tipeeestream.impl

import java.time.OffsetDateTime
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.util.{Currency, Locale}

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.dto.User
import org.codeoverflow.chatoverflow.api.io.dto.stat.stream.SubscriptionTier
import org.codeoverflow.chatoverflow.api.io.dto.stat.stream.tipeeestream._
import org.codeoverflow.chatoverflow.api.io.event.stream.tipeeestream._
import org.codeoverflow.chatoverflow.api.io.input.event.TipeeestreamEventInput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.impl.EventInputImpl
import org.codeoverflow.chatoverflow.requirement.service.tipeeestream.TipeeestreamConnector
import org.codeoverflow.chatoverflow.requirement.service.tipeeestream.TipeeestreamConnector._
import org.json.JSONException

import scala.reflect.ClassTag

@Impl(impl = classOf[TipeeestreamEventInput], connector = classOf[TipeeestreamConnector])
class TipeeestreamEventInputImpl extends EventInputImpl[TipeeestreamEvent, TipeeestreamConnector] with TipeeestreamEventInput with WithLogger {
  private val DATE_FORMATTER = new DateTimeFormatterBuilder()
    .parseCaseInsensitive().append(DateTimeFormatter.ISO_LOCAL_DATE_TIME).appendOffset("+HHMM", "Z").toFormatter

  override def start(): Boolean = {
    sourceConnector.get.registerEventHandler(handleExceptions(onFollow))
    sourceConnector.get.registerEventHandler(handleExceptions(onSubscription))
    sourceConnector.get.registerEventHandler(handleExceptions(onDonation))
    sourceConnector.get.registerEventHandler(handleExceptions(onCheer))
    sourceConnector.get.registerEventHandler(handleExceptions(onRaid))
    sourceConnector.get.registerEventHandler(handleExceptions(onHost))
    true
  }

  private def handleExceptions[T: ClassTag](handler: T => Unit): T => Unit = event => {
    try {
      handler(event)
    } catch {
      case e@(_: JSONException | _: IllegalArgumentException) =>
        val jsonClass = implicitly[ClassTag[T]].runtimeClass
        logger warn s"Error while parsing follow json of type ${jsonClass.getSimpleName}:"
        logger warn s"${e.getClass.getName} - ${e.getMessage}"
    }
  }

  private def onDonation(eventJson: DonationEventJSON): Unit = {
    val event = eventJson.json
    val parameter = event.getJSONObject("parameters")
    val user = new User(parameter.getString("username"))
    val message = parameter.optString("formattedMessage")
    val amount = parameter.getDouble("amount").toFloat
    val currency = if (parameter.has("currency")) Currency.getInstance(parameter.getString("currency"))
    else Currency.getInstance(Locale.getDefault)
    val time = OffsetDateTime.parse(event.getString("created_at"), DATE_FORMATTER)
    val donation = new TipeeestreamDonation(user, amount, currency, time, message)
    call(new TipeeestreamDonationEvent(donation))
  }

  private def onSubscription(eventJson: SubscriptionEventJSON): Unit = {
    val event = eventJson.json
    val parameter = event.getJSONObject("parameters")
    val user = new User(parameter.getString("username"))
    val time = OffsetDateTime.parse(event.getString("created_at"), DATE_FORMATTER)
    val resub = parameter.optInt("resub", 0)
    val provider = TipeeestreamProvider.parse(event.optString("origin"))
    val gifted = parameter.has("gifter")
    val donor = if (gifted) new User(parameter.getString("gifter")) else null
    val tier = SubscriptionTier.parse({
      if (parameter.optInt("twitch_prime") == 1)
        0
      else
        parameter.optInt("plan", 1000) / 1000
    })
    val sub = new TipeeestreamSubscription(user, time, resub, tier, gifted, donor, provider)
    call(new TipeeestreamSubscriptionEvent(sub))
  }

  private def onFollow(eventJson: FollowEventJSON): Unit = {
    val event = eventJson.json
    val parameter = event.getJSONObject("parameters")
    val user = new User(parameter.getString("username"))
    val time = OffsetDateTime.parse(event.getString("created_at"), DATE_FORMATTER)
    val provider = TipeeestreamProvider.parse(event.optString("origin"))
    val follow = new TipeeestreamFollow(user, time, provider)
    call(new TipeeestreamFollowEvent(follow))
  }

  private def onCheer(eventJson: CheerEventJSON): Unit = {
    val event = eventJson.json
    val parameter = event.getJSONObject("parameters")
    val user = new User(parameter.getString("username"))
    val time = OffsetDateTime.parse(event.getString("created_at"), DATE_FORMATTER)
    val amount = event.getInt("formattedAmount")
    val message = parameter.getString("formattedMessage")
    val cheer = new TipeeestreamCheer(user, amount, message, time)
    call(new TipeeestreamCheerEvent(cheer))
  }

  private def onRaid(eventJson: RaidEventJSON): Unit = {
    val event = eventJson.json
    val parameter = event.getJSONObject("parameters")
    val user = new User(parameter.getString("username"))
    val time = OffsetDateTime.parse(event.getString("created_at"), DATE_FORMATTER)
    val message = parameter.getString("formattedMessage")
    val viewers = parameter.getInt("viewers")
    val raid = new TipeeestreamRaid(user, message, viewers, time)
    call(new TipeeestreamRaidEvent(raid))
  }

  private def onHost(eventJson: HostEventJSON): Unit = {
    val event = eventJson.json
    val parameter = event.getJSONObject("parameters")
    val user = new User(parameter.getString("username"))
    val time = OffsetDateTime.parse(event.getString("created_at"), DATE_FORMATTER)
    val message = parameter.getString("formattedMessage")
    val viewers = parameter.getInt("viewers")
    val host = new TipeeestreamHost(user, message, viewers, time)
    call(new TipeeestreamHostEvent(host))
  }

  override def stop(): Boolean = {
    sourceConnector.get.unregisterAllEventListeners
    true
  }
}
