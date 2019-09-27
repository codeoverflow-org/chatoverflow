package org.codeoverflow.chatoverflow.requirement.service.tipeeestream

import org.codeoverflow.chatoverflow.requirement.impl.EventManager
import org.codeoverflow.chatoverflow.requirement.service.tipeeestream.TipeeestreamConnector._
import org.json.JSONObject

class TipeeestreamListener extends EventManager {

  def onSocketEvent(objects: Array[AnyRef]): Unit = {
    val json: JSONObject = objects(0).asInstanceOf[JSONObject]
    val event: JSONObject = json.getJSONObject("event")
    val eventType: String = event.getString("type")

    // Fire events for connector if we are looking for that type of event
    Option(eventType match {
      case "subscription" => call(SubscriptionEventJSON(event))
      case "donation" => call(DonationEventJSON(event))
      case "follow" => call(FollowEventJSON(event))
      case "cheer" => call(CheerEventJSON(event))
      case "raid" => call(RaidEventJSON(event))
      case "hosting" => call(HostEventJSON(event))
      case _ =>
    })
  }
}
