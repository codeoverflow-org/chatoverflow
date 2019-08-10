package org.codeoverflow.chatoverflow.requirement.service.discord

import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.react.{MessageReactionAddEvent, MessageReactionRemoveEvent}
import net.dv8tion.jda.core.events.message.{MessageDeleteEvent, MessageReceivedEvent, MessageUpdateEvent}
import net.dv8tion.jda.core.hooks.EventListener
import org.codeoverflow.chatoverflow.requirement.impl.EventManager

/**
  * The discord chat listener class holds the handler to react to creation, edits and removal of messages using the rest api
  */
class DiscordChatListener extends EventListener with EventManager {

  override def onEvent(event: Event): Unit = {
    val listeningClasses = List(classOf[MessageReceivedEvent], classOf[MessageUpdateEvent], classOf[MessageDeleteEvent],
      classOf[MessageReactionAddEvent], classOf[MessageReactionRemoveEvent])

    // Only broadcast event if we are listening for it (above list)
    if (listeningClasses.contains(event.getClass)) {
      call(event)
    }
  }
}
