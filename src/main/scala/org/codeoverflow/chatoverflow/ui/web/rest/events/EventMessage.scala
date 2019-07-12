package org.codeoverflow.chatoverflow.ui.web.rest.events

case class EventMessage[T](action: String, data: T)
