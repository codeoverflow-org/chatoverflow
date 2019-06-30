package org.codeoverflow.chatoverflow.requirement

import java.util.function.Consumer

import org.codeoverflow.chatoverflow.api.io.event.Event
import org.codeoverflow.chatoverflow.requirement.EventInputImpl.EventHandler

import scala.collection.mutable.ListBuffer

abstract class EventInputImpl {

  private val handlers = ListBuffer[EventHandler[_]]()

  def registerEventHandler[T <: Event](eventHandler: Consumer[T]): Unit = {
    handlers += new EventHandler[T](eventHandler)
  }

  def call[T <: Event](event: T): Unit = {
    handlers.filter(h => {
      h.clazz == classOf[T]
    }).foreach(h => h.consumer.accept(event)) // FIXME Type mismatch
  }
}
object EventInputImpl {
  private class EventHandler[T >: Event](val consumer: Consumer[T]) {
    val clazz: Class[T] = classOf[T]
  }
}
