package org.codeoverflow.chatoverflow.requirement.service.tipeeestream


import org.json.JSONObject

import scala.collection.mutable.ListBuffer

class TipeeestreamListener {

  private val subscriptionEventListeners = ListBuffer[JSONObject => Unit]()
  private val donationEventListeners = ListBuffer[JSONObject => Unit]()
  private val followEventListeners = ListBuffer[JSONObject => Unit]()

  def addSubscriptionEventListener(listener: JSONObject => Unit): Unit = {
    subscriptionEventListeners += listener
  }

  def addDonationEventListener(listener: JSONObject => Unit): Unit = {
    donationEventListeners += listener
  }

  def addFollowEventListener(listener: JSONObject => Unit): Unit = {
    followEventListeners += listener
  }

  def removeSubscriptionEventListener(listener: JSONObject => Unit): Unit = {
    subscriptionEventListeners -= listener
  }

  def removeDonationEventListener(listener: JSONObject => Unit): Unit = {
    donationEventListeners -= listener
  }

  def removeFollowEventListener(listener: JSONObject => Unit): Unit = {
    followEventListeners -= listener
  }

  def onSocketEvent(objects : Array[AnyRef]) : Unit = {
    val json: JSONObject = objects(0).asInstanceOf[JSONObject]
    val event: JSONObject = json.getJSONObject("event")
    val eventType: String = event.getString("type")
    eventType match {
      case "subscription" =>
        subscriptionEventListeners.foreach(_(event))
      case "donation" =>
        donationEventListeners.foreach(_(event))
      case "follow" =>
        followEventListeners.foreach(_(event))
      case _ =>
    }
  }
}
