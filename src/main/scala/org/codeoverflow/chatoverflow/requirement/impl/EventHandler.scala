package org.codeoverflow.chatoverflow.requirement.impl

import java.util.function.Consumer

import org.codeoverflow.chatoverflow.api.io.event.Event

private[impl] case class EventHandler[T <: Event](consumer: Consumer[T], clazz: Class[T])
