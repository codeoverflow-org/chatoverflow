package org.codeoverflow.chatoverflow.requirement.service.discord

import javax.security.auth.login.LoginException
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
  private val authTokenKey = "authToken"
  private val channelIdKey = "channelId"
  private var bot: JDA = _
  private var running = false
  private var currentChannel: Long = _ //TODO: Support for multiple channels
  requiredCredentialKeys = List(authTokenKey, channelIdKey)

  def addMessageEventListener(listener: MessageReceivedEvent => Unit): Unit = {
    discordChatListener.addMessageEventListener(listener)
  }

  def addMessageUpdateEventListener(listener: MessageUpdateEvent => Unit): Unit = {
    discordChatListener.addMessageUpdateEventListener(listener)
  }

  def addMessageDeleteEventListener(listener: MessageDeleteEvent => Unit): Unit = {
    discordChatListener.addMessageDeleteEventListener(listener)
  }

  override def isRunning: Boolean = running

  override def init(): Boolean = {
    if (!running) {
      logger info s"Starting connector for source '$sourceIdentifier' of type '$getUniqueTypeString'."

      if (!areCredentialsSet) {
        logger warn "No credentials set."
        false
      } else {
        val authToken = credentials.get.getValue(authTokenKey)
        if (authToken.isEmpty) {
          logger error  s"key '$authTokenKey' not found in credentials for '$sourceIdentifier'."
        }
        credentials.get.getValue(channelIdKey) match {
          case Some(value) => setChannel(value.toLong) //FIXME: Throws exception if not a valid snowflake id
          case None =>
            logger error  s"key '$channelIdKey' not found in credentials for '$sourceIdentifier'."
            return false
        }
        try {
          bot = new JDABuilder(authToken.getOrElse("")).build()
          logger info "Waiting while the bot is connecting..."
          bot.awaitReady()
          bot.addEventListener(discordChatListener)
          running = true
          logger info "Started connector."
          true
        } catch {
          case _: LoginException =>
            logger error s"Login failed! Invalid $authTokenKey."
            false
          case _: IllegalArgumentException =>
            logger warn s"Login failed! Empty $authTokenKey."
            false
        }
      }
    }
    else {
      logger warn "Connector already running."
      false
    }
  }

  def setChannel(channelId: Long): Unit = {
    currentChannel = channelId
  }

  override def shutdown(): Unit = {
    bot.shutdown()
    logger info s"Stopped connector for source '$sourceIdentifier' of type '$getUniqueTypeString'."
  }

  override def getUniqueTypeString: String = this.getClass.getName

  //FIXME: getTextChannelById may return null for invalid channelIds
  def sendChatMessage(chatMessage: String): Unit = bot.getTextChannelById(currentChannel).sendMessage(chatMessage).queue()
}