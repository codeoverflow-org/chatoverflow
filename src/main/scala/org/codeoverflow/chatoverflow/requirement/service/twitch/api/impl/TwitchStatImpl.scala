package org.codeoverflow.chatoverflow.requirement.service.twitch.api.impl

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import org.codeoverflow.chatoverflow.api.io.dto.service.User
import org.codeoverflow.chatoverflow.api.io.input.stat.TwitchStatInput
import org.codeoverflow.chatoverflow.framework.actors.{Mapping, StringMappingActor}
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.Connection
import org.codeoverflow.chatoverflow.requirement.service.twitch.api.TwitchAPIConnector

import scala.concurrent.Await
import scala.concurrent.duration._

// FIXME: Chery picked from Class Library Rework, should be reworked, lol

case class UserResult(data: Seq[UserEntity])

case class UserEntity(id: String, login: String, display_name: String, `type`: String, broadcaster_type: String, description: String, profile_image_url: String, offline_image_url: String, view_count: Int)

@Impl(impl = classOf[TwitchStatInput], connector = classOf[TwitchAPIConnector])
class TwitchStatInputImpl extends Connection[TwitchAPIConnector] with TwitchStatInput {
  private val actorSystem = ActorSystem("TwitchAPIActorSystem")
  private val actor: ActorRef = actorSystem.actorOf(Props[StringMappingActor])
  implicit val timeout: Timeout = Timeout(5 seconds)

  override def init(): Boolean = {
    sourceConnector.get.init()
  }

  override def getFollowers(userName: String): java.util.List[User] = {
    val userID = getUser(userName).getId
    val response = sourceConnector.get.getFollowers(userID)
    println(response)
    null
  }

  override def getUser(userName: String): User = {
    val response = sourceConnector.get.getUser(userName)
    println(response)
    val result = Await.result(actor ? Mapping(map[UserResult], response), timeout.duration).asInstanceOf[UserResult]
    if (result.data.nonEmpty) {
      val user = result.data.head
      new User(user.id, user.display_name, user.description, user.profile_image_url, user.view_count)
    }
    else null
  }

  // FIXME: Kicked jackson mapping (deprecated?!), different way needed
  def map[T: Manifest](content: String): Any = {
    //val mapper = new ObjectMapper() with ScalaObjectMapper
    // mapper.registerModule(DefaultScalaModule)
    //mapper.readValue[T](content)
  }

  override def getSubscribers(userName: String): String = {
    val userID = getUser(userName).getId
    sourceConnector.get.getSubscriptions(userID)
  }

  override def serialize(): String = ???

  override def deserialize(value: String): Unit = ???
}
