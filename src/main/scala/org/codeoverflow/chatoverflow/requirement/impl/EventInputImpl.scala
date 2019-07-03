package org.codeoverflow.chatoverflow.requirement.impl

import java.util.function.Consumer

import org.codeoverflow.chatoverflow.api.io.event.Event
import org.codeoverflow.chatoverflow.api.io.input.event.EventInput
import org.codeoverflow.chatoverflow.connector.Connector

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

/**
  * Default implementation for all inputs that provide events.
  *
  * The integrated event registry allows registering new Event handlers at any time.
  * @tparam T the event interface that all events for this EventInput share
  * @tparam C the connector to which this input belongs
  */
abstract class EventInputImpl[T <: Event, C <: Connector](implicit ctc: ClassTag[C]) extends InputImpl[C] with EventInput[T] {

  protected val handlers: ListBuffer[EventHandler[_ <: T]] = ListBuffer[EventHandler[_ <: T]]()

  override def registerEventHandler[S <: T](eventHandler: Consumer[S], eventClass: Class[S]): Unit = {
    handlers += EventHandler[S](eventHandler, eventClass)
  }

  protected def call[S <: T](event: S)(implicit cts: ClassTag[S]): Unit = {
    handlers.filter(handler => handler.clazz == cts.runtimeClass)
      .foreach(handler => handler.consumer.asInstanceOf[Consumer[S]].accept(event))
  }
}
