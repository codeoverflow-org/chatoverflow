package org.codeoverflow.chatoverflow.requirement

import java.util.function.Consumer

import org.codeoverflow.chatoverflow.api.io.event.Event
import org.codeoverflow.chatoverflow.requirement.EventInputImpl.EventHandler

import scala.collection.mutable.ListBuffer
import scala.reflect.runtime.universe

abstract class EventInputImpl {

  private val handlers = ListBuffer[EventHandler[_]]()

  def registerEventHandler[T <: Event](implicit tt: universe.TypeTag[T],  eventHandler: Consumer[T]): Unit = {
    handlers += new EventHandler[T](tt, eventHandler)
  }

  def call[T <: Event](implicit tt: universe.TypeTag[T], event: T): Unit = {
    handlers.filter(h => {
      h.clazz == classOf[T]
    }).foreach(h => h.consumer.accept(h.cast(event))) // TODO Test if working
  }
}
object EventInputImpl {
  private class EventHandler[T <: Event](private val tt: universe.TypeTag[T], val consumer: Consumer[T]) {
    val clazz: Class[T] = classOf[T]

    def cast[A](a: Any): A = a.asInstanceOf[A]
  }
}
