package org.codeoverflow.chatoverflow.connector


import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.configuration.Credentials
import org.codeoverflow.chatoverflow.connector.actor.ActorMessage

import scala.concurrent.duration._
import scala.concurrent.{Await, TimeoutException}
import scala.language.{implicitConversions, postfixOps}
import scala.reflect.ClassTag

/**
  * A connector is used to connect a input / output service to its dedicated platform
  *
  * @param sourceIdentifier the unique source identifier (e.g. a login name), the connector should work with
  */
abstract class Connector(val sourceIdentifier: String) extends WithLogger {
  private[this] val actorSystem: ActorSystem = ActorSystem(s"${getUniqueTypeString.replace('.', '-')}")
  private val connectorSourceAndType = s"connector '$sourceIdentifier' of type '$getUniqueTypeString'"
  protected var credentials: Option[Credentials] = None
  protected var requiredCredentialKeys: List[String]
  protected var optionalCredentialKeys: List[String]
  protected var running = false
  protected implicit val identifier: String = hashCode().toString

  private var instanceCount = 0

  def getCredentials: Option[Credentials] = this.credentials

  /**
    * Sets the credentials needed for login or authentication of the connector to its platform
    *
    * @param credentials the credentials object to login to the platform
    */
  def setCredentials(credentials: Credentials): Unit = this.credentials = Some(credentials)

  /**
    * Removes the entire credentials object from the connector.
    */
  def removeCredentials(): Unit = this.credentials = None

  def setCredentialsValue(key: String, value: String): Boolean = {
    if (credentials.isEmpty) false else {
      if (credentials.get.exists(key)) {
        credentials.get.removeValue(key)
      }
      credentials.get.addValue(key, value)
      true
    }
  }

  /**
    * Returns the keys that should be set in the credentials object
    *
    * @return a list of keys
    */
  def getRequiredCredentialKeys: List[String] = requiredCredentialKeys

  /**
    * Returns the keys that are optional to be set in the credentials object
    *
    * @return a list of keys
    */
  def getOptionalCredentialKeys: List[String] = optionalCredentialKeys

  /**
    * Returns true, if the connector has been already instantiated and is running.
    */
  def isRunning: Boolean = running

  /**
    * Initializes the connector by checking the conditions and then calling the start method.
    */
  def init(): Boolean = {

    // Check if running
    if (running) {
      logger info s"Unable to start $connectorSourceAndType. Already running!"
      changeInstanceCount(1)
      true
    } else {

      // Check if credentials object exists
      if (!areCredentialsSet) {
        logger warn s"Unable to start $connectorSourceAndType. Credentials object not set."
        false
      } else {

        val unsetCredentials = for (key <- requiredCredentialKeys if !credentials.get.exists(key)) yield key

        // Check required credentials
        if (unsetCredentials.nonEmpty) {
          logger warn s"Unable to start $connectorSourceAndType. Not all required credentials are set."

          logger info s"Not set credentials are: ${unsetCredentials.mkString(", ")}."
          false
        } else {

          if (!optionalCredentialKeys.forall(key => credentials.get.exists(key))) {
            logger info "There are unset optional credentials."
          }

          if (start()) {
            logger info s"Started $connectorSourceAndType."
            changeInstanceCount(1)
            running = true
            true
          } else {
            logger warn s"Failed starting $connectorSourceAndType."
            false
          }
        }
      }
    }
  }

  /**
    * Returns if the credentials had been set. Can be asked before running the init()-function.
    *
    * @return true if the credentials are not none. Does say nothing about their value quality
    */
  def areCredentialsSet: Boolean = credentials.isDefined

  /**
    * Starts the connector, e.g. creates a connection with its platform.
    */
  def start(): Boolean

  /**
    * Shuts down the connector by calling the stop method.
    */
  def shutdown(): Boolean = {
    changeInstanceCount(-1)

    if (instanceCount <= 0) {
      logger info "Instance count is zero. Connector is no longer needed and shut down. RIP."
      if (stop()) {
        running = false
        logger info s"Stopped $connectorSourceAndType."
        true
      } else {
        logger warn s"Unable to shutdown $connectorSourceAndType."
        false
      }
    } else {
      true // Return true to keep transparency
    }
  }

  private def changeInstanceCount(delta: Int): Unit = {
    instanceCount += delta
    logger info s"Instance count: $instanceCount"
  }

  /**
    * Returns the unique type string of the implemented connector.
    *
    * @return the class type
    */
  def getUniqueTypeString: String = this.getClass.getName

  /**
    * This stops the activity of the connector, e.g. by closing the platform connection.
    */
  def stop(): Boolean

  /**
    * This method can be used to ask an actor for an result, using the akka system as a black box.
    *
    * @param actor            the specific actor to ask. Use <code>createActor()</code> to create your actor.
    * @param timeOutInSeconds the timeout to calculate, request, ... for the actor
    * @param message          some message to pass to the actor. Can be anything.
    * @tparam T result type of the actor answer
    * @return the answer of the actor if he answers in time. else: None
    */
  def askActor[T](actor: ActorRef, timeOutInSeconds: Int, message: ActorMessage): Option[T] = {
    implicit val timeout: Timeout = Timeout(timeOutInSeconds seconds)
    val future = actor ? message
    try {
      Some(Await.result(future, timeout.duration).asInstanceOf[T])
    } catch {
      case _: TimeoutException => None
    }
  }

  /**
    * Creates a new actor of the given type and returns the reference. Uses the connector specific actor system.
    *
    * @tparam T the type of the desired actor (possible trough scala magic)
    * @return a actor reference, ready to be used
    */
  protected def createActor[T <: Actor : ClassTag](): ActorRef =
    actorSystem.actorOf(Props(implicitly[ClassTag[T]].runtimeClass))

  implicit def toRichActorRef(actorRef: ActorRef): RichActorRef = new RichActorRef(actorRef)

  class RichActorRef(actorRef: ActorRef) {

    /**
      * Syntactic sugar for the askActor()-function. Works like this:
      * <code>anActor.??[String](5) {
      * // message goes here
      * }</code>
      *
      * @param timeOutInSeconds the timeout to calculate, request, ... for the actor
      * @param message          some message to pass to the actor. Can be anything.
      * @tparam T result type of the actor answer
      * @return the answer of the actor if he answers in time. else: None
      */
    def ??[T](timeOutInSeconds: Int)(message: ActorMessage): Option[T] = {
      askActor[T](actorRef, timeOutInSeconds, message)
    }
  }

}