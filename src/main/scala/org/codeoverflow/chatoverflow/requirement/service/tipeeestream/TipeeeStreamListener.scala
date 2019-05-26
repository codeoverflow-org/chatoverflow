package org.codeoverflow.chatoverflow.requirement.service.tipeeestream

import org.codeoverflow.chatoverflow.api.io.dto.event.tipeeestream.TipeeeStreamEvent

import scala.collection.mutable.ListBuffer

class TipeeeStreamListener {

  private val messageEventListener = ListBuffer[TipeeeStreamEvent => Unit]()

  def onMessage(event: TipeeeStreamEvent): Unit = {
    messageEventListener.foreach(listener => listener(event))
  }

  def addMessageEventListener(listener: TipeeeStreamEvent => Unit): Unit = {
    messageEventListener += listener
  }
}
