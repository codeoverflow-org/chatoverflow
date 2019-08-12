package org.codeoverflow.chatoverflow.requirement.service.tipeeestream

import org.codeoverflow.chatoverflow.requirement.impl.EventManager
import org.codeoverflow.chatoverflow.requirement.service.tipeeestream.TipeeestreamConnector._
import org.json.JSONObject

class TipeeestreamListener extends EventManager {

  def onSocketEvent(objects: Array[AnyRef]): Unit = {
    val json: JSONObject = objects(0).asInstanceOf[JSONObject]
    val event: JSONObject = json.getJSONObject("event")
    val eventType: String = event.getString("type")

    val eventOption: Option[TipeeestreamEventJSON] = Option(eventType match {
      case "subscription" => SubscriptionEventJSON(event)
      case "donation" => DonationEventJSON(event)
      case "follow" => FollowEventJSON(event)
      case _ => null // gets None if converted to an option
    })

    if (eventOption.isDefined)
      call(eventOption.get) // send event to connector
  }
}
