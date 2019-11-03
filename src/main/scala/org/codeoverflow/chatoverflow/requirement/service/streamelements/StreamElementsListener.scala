package org.codeoverflow.chatoverflow.requirement.service.streamelements

import org.codeoverflow.chatoverflow.requirement.impl.EventManager
import org.codeoverflow.chatoverflow.requirement.service.streamelements.StreamElementsConnector._
import org.json.JSONObject

/**
 * Listener for websocket events that are emitted by the StreamElements websocket server.
 */
class StreamElementsListener extends EventManager {

  def handleEvent(objects: Array[AnyRef]): Unit = {
    val json = objects(0).asInstanceOf[JSONObject]

    val eventType = json.optString("type")
    if (eventType != null) {
      val provider = json.getString("provider")

      eventType match {
        // Youtube's description differs from the usual ones.
        // Youtube's subscription is more like a follow of e.g. Twitch or Twitter, it is free for the user,
        // Youtube's sponsor is like a Twitch subscription, a paid extra for some perks and
        // Youtube's superchat is like a donation/tip, a donation of money that the streamer gets.
        // To unify this across all platforms a Youtube sub is a Follow, a Youtube sponsor is a subscription and
        // a Youtube superchat is a donation.
        case "subscriber" if provider == "youtube" => call(FollowEventJSON(json))
        case "sponsor" => call(SubscriptionEventJSON(json))
        case "superchat" => call(DonationEventJSON(json))

        // Twitch
        case "subscriber" => call(SubscriptionEventJSON(json))
        case "tip" => call(DonationEventJSON(json))
        case "follow" => call(FollowEventJSON(json))
        case "cheer" => call(CheerEventJSON(json))
        case "host" => call(HostEventJSON(json))
        case "raid" => call(RaidEventJSON(json))
        case _ =>
      }
    }
  }
}
