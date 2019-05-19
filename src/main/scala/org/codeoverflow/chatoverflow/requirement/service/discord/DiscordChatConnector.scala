package org.codeoverflow.chatoverflow.requirement.service.discord

import java.util.function.Consumer

import javax.security.auth.login.LoginException
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.{MessageDeleteEvent, MessageReceivedEvent, MessageUpdateEvent}
import net.dv8tion.jda.api.{JDA, JDABuilder}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector

/**
  * The discord connector connects to the discord REST API
  *
  * @param sourceIdentifier the unique source identifier (in this implementation only for identifying)
  */
class DiscordChatConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {
  private val discordChatListener = new DiscordChatListener
  private val defaultFailureHandler: Consumer[_ >: Throwable] =
    throwable => logger warn s"Rest action for connector $sourceIdentifier failed: ${throwable.getMessage}"
  override protected var requiredCredentialKeys: List[String] = List("authToken")
  override protected var optionalCredentialKeys: List[String] = List()
  private var jda: Option[JDA] = None

  def addMessageReceivedListener(listener: MessageReceivedEvent => Unit): Unit = {
    discordChatListener.addMessageReceivedListener(listener)
  }

  def addMessageUpdateListener(listener: MessageUpdateEvent => Unit): Unit = {
    discordChatListener.addMessageUpdateEventListener(listener)
  }

  def addMessageDeleteListener(listener: MessageDeleteEvent => Unit): Unit = {
    discordChatListener.addMessageDeleteEventListener(listener)
  }

  /**
    * Connects to discord
    */
  override def start(): Boolean = {
    try {
      jda = Some(new JDABuilder(credentials.get.getValue("authToken").get).build())
      jda.get.addEventListener(discordChatListener)
      logger info "Waiting while the bot is connecting..."
      jda.get.awaitReady()
      true
    } catch {
      case _: LoginException =>
        logger warn "Login failed! Invalid authToken."
        false
      case _: IllegalArgumentException =>
        logger warn "Login failed! Empty authToken."
        false
    }
  }

  /**
    * Closes the connection to discord
    */
  override def stop(): Boolean = {
    jda.foreach(_.shutdown())
    true
  }

  /**
    * Retrieves a text channel
    *
    * @param channelId the id of a text channel
    * @return Some text channel or None if no text channel with that id exists
    */
  def getTextChannel(channelId: String): Option[TextChannel] = Option(validJDA.getTextChannelById(channelId))

  /**
    * validates that jda is currently available
    *
    * @return the jda instance
    * @throws IllegalStateException if JDA is not available yet
    */
  private def validJDA: JDA = {
    jda match {
      case Some(_jda) => _jda
      case None => throw new IllegalStateException("JDA is not available yet")
    }
  }

  /**
    * Sends a message to a text channel
    *
    * @param channelId   the id of the text channel
    * @param chatMessage the actual message
    */
  def sendChatMessage(channelId: String, chatMessage: String): Unit = {
    Option(validJDA.getTextChannelById(channelId)) match {
      case Some(channel) => channel.sendMessage(chatMessage).queue(null, defaultFailureHandler)
      case None => throw new IllegalArgumentException(s"Channel with id $channelId not found")
    }
  }
}