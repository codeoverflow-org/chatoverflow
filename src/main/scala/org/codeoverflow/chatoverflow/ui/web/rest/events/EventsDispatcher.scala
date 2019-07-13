package org.codeoverflow.chatoverflow.ui.web.rest.events

import org.json4s.{DefaultFormats, Formats}
import org.json4s.jackson.Serialization

/**
  * The EventsDispatcher is the central point for realtime communication to the clients
  */
object EventsDispatcher {
  private var controller: EventsController = _
  implicit val formats: Formats = DefaultFormats

  /**
    * Initializes the EventsDispatcher with the registered controller
    * Only to be used from the bootstrap
    * @param eventsController registered controller that accepts the incoming connections
    */
  def init(eventsController: EventsController): Unit = {
    if (controller == null)
      controller = eventsController
  }

  /**
    * Sends the message to all connected clients
    * @param messageType type of the message / event
    * @param message the message to send
    * @tparam T type of the message data
    */
  def broadcast[T](messageType: String, message: EventMessage[T]): Unit = {
    broadcast(messageType, Serialization.write(message))
  }

  /**
    * Sends the message to all connected clients
    * @param messageType type of the message / event
    * @param message the message to send
    */
  def broadcast(messageType: String, message: String = null): Unit = {
    if (controller != null)
      controller.broadcast(messageType, message)
  }

  /**
    * Sends a close message to all connected clients and closes the connections
    */
  def close(): Unit = {
    if (controller != null)
      controller.closeConnections()
  }
}
