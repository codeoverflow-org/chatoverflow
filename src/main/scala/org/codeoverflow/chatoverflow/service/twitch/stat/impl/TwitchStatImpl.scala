package org.codeoverflow.chatoverflow.service.twitch.stat.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.codeoverflow.chatoverflow.api.io.input.chat.User
import org.codeoverflow.chatoverflow.api.io.input.stat.TwitchStatInput
import org.codeoverflow.chatoverflow.service.Connection
import org.codeoverflow.chatoverflow.service.twitch.TwitchAPIConnector

case class UserResult(data: Seq[UserEntity])
case class UserEntity(id: String, login: String, display_name: String, `type`: String, broadcaster_type: String, description: String, profile_image_url: String, offline_image_url: String, view_count: Int)

class TwitchStatInputImpl extends Connection[TwitchAPIConnector] with TwitchStatInput {
  override def init(): Unit = {
    sourceConnector.init()
  }

  override def getFollowers(userName: String): java.util.List[User] = {
    val userID = getUser(userName).getId
    val response = sourceConnector.getFollowers(userID)
    println(response)
    null
  }

  override def getUser(userName: String): User = {
    val response = sourceConnector.getUser(userName)
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val result = mapper.readValue[UserResult](response)
    if (result.data.nonEmpty) {
      val user = result.data.head
      new User(user.id, user.display_name, user.description, user.profile_image_url, user.view_count)
    }
    else null
  }

  override def getSubscribers(userName: String): String = {
    val userID = getUser(userName).getId
    sourceConnector.getSubscriptions(userID)
  }
}
