package org.codeoverflow.chatoverflow.requirement.service.tipeeestream.impl

import java.time.OffsetDateTime
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.util
import java.util.Currency

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.dto.User
import org.codeoverflow.chatoverflow.api.io.dto.stat.stream.tipeeestream.{TipeeestreamDonation, TipeeestreamFollow, TipeeestreamProvider, TipeeestreamProviderDetails, TipeeestreamStats, TipeeestreamSubscription}
import org.codeoverflow.chatoverflow.api.io.input.stat.TipeeestreamStatInput
import org.codeoverflow.chatoverflow.requirement.impl.InputImpl
import org.codeoverflow.chatoverflow.requirement.service.tipeeestream.TipeeestreamConnector
import org.json4s._

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class TipeeestreamStatInputImpl extends InputImpl[TipeeestreamConnector] with TipeeestreamStatInput with WithLogger {

  private val DATE_FORMATTER = new DateTimeFormatterBuilder()
    .parseCaseInsensitive().append(DateTimeFormatter.ISO_LOCAL_DATE_TIME).appendOffset("+HHMM", "Z").toFormatter


  override def start(): Boolean = true

  override def stop(): Boolean = true

  override def getLastFollows(start: OffsetDateTime): util.List[TipeeestreamFollow] = {
    implicit val formats = DefaultFormats
    val responses = sourceConnector.get.getRecentEvents(Seq("follow"), start)
    val follows = ListBuffer[TipeeestreamFollow]()
    for (json <- responses) {
      (json \ "datas" \ "items").asInstanceOf[JArray].arr.foreach(event => {
        val follower = (event \ "parameters" \ "username").extract[String]
        val time = OffsetDateTime.parse((event \ "created_at").extract[String], DATE_FORMATTER)
        val provider = TipeeestreamProvider.parse((event \ "origin").extract[String])
        follows += new TipeeestreamFollow(new User(follower), time, provider)
      })
    }
    follows.toList.asJava
  }

  override def getLastSubscriptions(start: OffsetDateTime): util.List[TipeeestreamSubscription] = {
    implicit val formats = DefaultFormats
    val responses = sourceConnector.get.getRecentEvents(Seq("subscription"), start)
    val subscriptions = ListBuffer[TipeeestreamSubscription]()
    for (json <- responses) {
      (json \ "datas" \ "items").asInstanceOf[JArray].arr.foreach(event => {
        val subscriber = (event \ "parameters" \ "username").extract[String]
        val resub = (event \ "parameters" \ "resub").extract[Int]
        val time = OffsetDateTime.parse((event \ "created_at").extract[String], DATE_FORMATTER)
        val provider = TipeeestreamProvider.parse((event \ "origin").extract[String])
        subscriptions += new TipeeestreamSubscription(new User(subscriber), resub, time, provider)
      })
    }
    subscriptions.toList.asJava
  }

  override def getLastDonations(start: OffsetDateTime): util.List[TipeeestreamDonation] = {
    implicit val formats = DefaultFormats
    val responses = sourceConnector.get.getRecentEvents(Seq("donation"), start)
    val donations = ListBuffer[TipeeestreamDonation]()
    for (json <- responses) {
      (json \ "datas" \ "items").asInstanceOf[JArray].arr.foreach(event => {
        val donor = (event \ "parameters" \ "username").extract[String]
        val amount = (event \ "parameters" \ "amount").extract[Double].toFloat
        val currency = Currency.getInstance((event \ "parameters" \ "currency").extract[String])
        val time = OffsetDateTime.parse((event \ "created_at").extract[String], DATE_FORMATTER)
        val message = (event \ "parameters" \ "formattedMessage").extract[String]
        donations += new TipeeestreamDonation(new User(donor), amount, currency, time, message)
      })
    }
    donations.toList.asJava
  }

  override def getStats: TipeeestreamStats = {
    try {
      implicit val formats = DefaultFormats
      val accountInfo: JValue = sourceConnector.get.getAccountInfo
      val eventStats = sourceConnector.get.getEventStats \ "datas"

      val identifier = (accountInfo \ "id").extract[Int].toString
      val name = (accountInfo \ "username").extract[String]
      val avatar = "https://www.tipeeestream.com/v1.0/media/download/" + (accountInfo \ "avatar").extract[String]
      val creationTime = OffsetDateTime.parse((accountInfo \ "created_at").extract[String], DATE_FORMATTER)

      val subscribersTotal = (eventStats \ "subscribers").extractOrElse(0)
      val followersTotal = (eventStats \ "followers").extractOrElse(0)
      val dontionsTotal = (eventStats \ "donations").extractOrElse(0)

      val details = (accountInfo \ "providers").asInstanceOf[JArray].arr.map(provider => {
        val code = (provider \ "code").extract[String]
        val pType = TipeeestreamProvider.parse(code)
        val pIdentifier = (provider \ "id").extract[String]
        val pName = (provider \ "username").extract[String]
        val pChannelUrl = (provider \ "channel").extract[String]
        val pCreationTime = OffsetDateTime.parse((provider \ "created_at").extract[String], DATE_FORMATTER)
        val pFollowers = (accountInfo \ "details" \ code \ "followers").extractOrElse(0)
        val pSubscribers = (accountInfo \ "details" \ code \ "subscribers").extractOrElse(0)
        new TipeeestreamProviderDetails(pType, pIdentifier, pName, pChannelUrl, pCreationTime, pFollowers, pSubscribers)
      }).asJava
      new TipeeestreamStats(identifier, name, avatar, creationTime, subscribersTotal, followersTotal, dontionsTotal, details)
    } catch {
      case e: Exception => throw new RuntimeException("Could not parse tipeeestream stats json", e)
    }
    }
}
