package org.codeoverflow.chatoverflow.requirement.service.streamelements.impl

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.Currency

import org.codeoverflow.chatoverflow.api.io.dto.User
import org.codeoverflow.chatoverflow.api.io.dto.stat.stream.SubscriptionTier
import org.codeoverflow.chatoverflow.api.io.dto.stat.stream.streamelements.{StreamElementsDonation, StreamElementsFollow, StreamElementsProvider, StreamElementsSubscription}
import org.codeoverflow.chatoverflow.api.io.event.stream.streamelements.{StreamElementsDonationEvent, StreamElementsEvent, StreamElementsFollowEvent, StreamElementsSubscriptionEvent}
import org.codeoverflow.chatoverflow.api.io.input.event.StreamElementsEventInput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.impl.EventInputImpl
import org.codeoverflow.chatoverflow.requirement.service.streamelements.StreamElementsConnector
import org.codeoverflow.chatoverflow.requirement.service.streamelements.StreamElementsConnector._
import org.json.{JSONException, JSONObject}

import scala.reflect.ClassTag

@Impl(impl = classOf[StreamElementsEventInput], connector = classOf[StreamElementsConnector])
class StreamElementsEventInputImpl extends EventInputImpl[StreamElementsEvent, StreamElementsConnector] with StreamElementsEventInput {

  override def start(): Boolean = {
    sourceConnector.get.registerEventHandler(handleExceptions(onFollow))
    sourceConnector.get.registerEventHandler(handleExceptions(onSubscription))
    sourceConnector.get.registerEventHandler(handleExceptions(onDonation))
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

  private def onFollow(event: FollowEventJSON): Unit = {
    val json = event.json
    val data = json.getJSONObject("data")

    val follow = new StreamElementsFollow(
      parseUser(data),
      parseTime(json),
      parseProvider(json)
    )
    call(new StreamElementsFollowEvent(follow))
  }

  private def onSubscription(event: SubscriptionEventJSON): Unit = {
    val json = event.json
    val data = json.getJSONObject("data")

    val gifted = data.optBoolean("gifted", false)
    val sub = new StreamElementsSubscription(
      parseUser(data),
      parseTime(json),
      data.optDouble("amount", 1).toInt,
      {
        // (judging based on the events from the event simulator that can be seen in the browser console)
        // "plan" can be either a string or a number, so we need to handle both cases
        val plan = Option(data.opt("plan")).getOrElse("1000").toString
        if (plan.toLowerCase == "prime")
          SubscriptionTier.PRIME
        else
          SubscriptionTier.parse(plan.toInt / 1000)
      },
      gifted,
      if (gifted) new User(data.optString("sender")) else null,
      parseProvider(json)
    )
    call(new StreamElementsSubscriptionEvent(sub))
  }

  private def onDonation(event: DonationEventJSON): Unit = {
    val json = event.json
    val data = json.getJSONObject("data")

    val donation = new StreamElementsDonation(
      parseUser(data),
      data.getDouble("amount").toFloat,
      Currency.getInstance(data.getString("currency")),
      parseTime(json),
      data.getString("message")
    )
    call(new StreamElementsDonationEvent(donation))
  }

  override def stop(): Boolean = {
    sourceConnector.get.unregisterAllEventListeners
    true
  }

  // Common methods for JSON processing:

  private def parseProvider(json: JSONObject): StreamElementsProvider = StreamElementsProvider.parse(json.getString("provider"))

  private def parseUser(json: JSONObject): User = {
    val username = json.getString("username")
    val displayName = json.optString("displayName", username)
    new User(username, displayName)
  }

  private def parseTime(json: JSONObject): OffsetDateTime = {
    val utcString = json.getString("createdAt")
    LocalDateTime.parse(utcString, DateTimeFormatter.ISO_DATE_TIME).atOffset(ZoneOffset.UTC)
  }
}
