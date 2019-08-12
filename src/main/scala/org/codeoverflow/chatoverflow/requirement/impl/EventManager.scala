package org.codeoverflow.chatoverflow.requirement.impl

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

/**
 * A class with common methods for event managing like register methods and a protected method to fire events.
 */
trait EventManager {
  private val eventHandlers = ListBuffer[EventHandler[_]]()

  /**
   * Fires a event and calls all registered handlers.
   *
   * @param eventValue the actual value of the event.
   * @param classTag   a class tag to be able to have access to the class of the event after type erasure.
   * @tparam T the type/class of the event
   */
  protected def call[T: ClassTag](eventValue: T)(implicit classTag: ClassTag[T]): Unit = {
    eventHandlers.foreach(_.handle(eventValue))
  }

  /**
   * Registers a event handler for the given type and sub-types.
   *
   * @param handler    the handler that will be called if this type of event gets fired
   * @param identifier a identifier of the registerer, used by the unregister method to identify who registered what.
   * @tparam T the type for which you want to
   */
  def registerEventHandler[T: ClassTag](handler: T => Unit)(implicit identifier: String): Unit = {
    eventHandlers += SingleEventHandler(identifier, handler)
  }

  /**
   * Registers a event handler that will receive all events.
   *
   * @param handler    the handler that will be called if a event gets fired, includes a class tag to be able
   *                   to get the class of the event.
   * @param identifier a identifier of the registerer, used by the unregister method to identify who registered what.
   */
  def registerEventHandler(handler: (Any, ClassTag[Any]) => Unit)(implicit identifier: String): Unit = {
    eventHandlers += AllEventHandler(identifier, handler)
  }

  /**
   * Unregisters all handlers that were registered by the passed identifier.
   *
   * @param identifier a identifier of the registerer, anything registered with this identifier will be unregistered.
   */
  def unregisterAllEventListeners(implicit identifier: String): Unit = {
    eventHandlers --= eventHandlers.filter(_.identifier == identifier)
  }

  /**
   * A instance containg all required information about a event handler with a method to actually handle events.
   *
   * @param identifier the identifier used by the unregister method
   * @param ct         the class tag to get a instance of the class of type T at runtime
   * @tparam T the type of the event that this handler wants to handle
   */
  private abstract class EventHandler[T: ClassTag](val identifier: String)(implicit val ct: ClassTag[T]) {
    /**
     * Handles the passed event value. Also has to determine if the handler actually wants to handle this event
     *
     * @param eventValue    the value of the event
     * @param eventClassTag the class tag to get the class of the event
     */
    def handle(eventValue: Any)(implicit eventClassTag: ClassTag[T]): Unit
  }

  /**
   * A event handler that only wants to handle a single specific type of events.
   *
   * @param identifier the identifier used by the unregister method
   * @param handler    a simple anonymous function to handle the event
   * @param ct         the class tag to get a instance of the class of type T at runtime
   * @tparam T the type of the event that this handler wants to handle
   */
  private case class SingleEventHandler[T: ClassTag](override val identifier: String, handler: T => Unit)(implicit ct: ClassTag[T]) extends EventHandler[T](identifier)(ct, ct) {
    override def handle(eventValue: Any)(implicit eventClassTag: ClassTag[T]): Unit = {
      if (ct.runtimeClass.isAssignableFrom(eventClassTag.runtimeClass))
        handler.asInstanceOf[T => Unit].apply(eventValue.asInstanceOf[T])
    }
  }

  /**
   * A event handler that handles all types of events.
   *
   * @param identifier the identifier used by the unregister method
   * @param handler    a simple anonymous function to handle the event, also gets a class tag passed to identify the class at runtime.
   * @tparam T the type of the event that this handler wants to handle
   */
  private case class AllEventHandler[T: ClassTag](override val identifier: String, handler: (Any, ClassTag[Any]) => Unit) extends EventHandler[T](identifier) {
    override def handle(eventValue: Any)(implicit eventClassTag: ClassTag[T]): Unit = {
      handler(eventValue, eventClassTag.asInstanceOf[ClassTag[Any]])
    }
  }

}
