package org.codeoverflow.chatoverflow.ui.web.rest.events

object EventsDispatcher {
  private var controller: EventsController = _

  def init(eventsController: EventsController): Unit = {
    if (controller == null)
      controller = eventsController
  }

  def broadcast(messageType: String, message: String = null): Unit = {
    if (controller != null)
      controller.broadcast(messageType, message)
  }

  def close(): Unit = {
    if (controller != null)
      controller.closeConnections()
  }
}
