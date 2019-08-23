package org.codeoverflow.chatoverflow.requirement.service.discord

import java.io.File
import java.util.function.Consumer

import javax.security.auth.login.LoginException
import net.dv8tion.jda.core.entities.{Message, MessageEmbed, TextChannel}
import net.dv8tion.jda.core.requests.RestAction
import net.dv8tion.jda.core.{JDA, JDABuilder, MessageBuilder}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.EventConnector
import org.codeoverflow.chatoverflow.connector.actor.FileSystemActor
import org.codeoverflow.chatoverflow.connector.actor.FileSystemActor.LoadBinaryFile

/**
  * The discord connector connects to the discord REST API
  *
  * @param sourceIdentifier the unique source identifier (in this implementation only for identifying)
  */
class DiscordChatConnector(override val sourceIdentifier: String) extends EventConnector(sourceIdentifier) with WithLogger {
  private val discordChatListener = new DiscordChatListener

  private var jda: Option[JDA] = None

  private val fileSystemActor = createActor[FileSystemActor]

  override protected var requiredCredentialKeys: List[String] = List("authToken")
  override protected var optionalCredentialKeys: List[String] = List()

  private val defaultFailureHandler: Consumer[_ >: Throwable] =
    throwable => logger warn s"Rest action for connector $sourceIdentifier failed: ${throwable.getMessage}"

  discordChatListener.registerEventHandler((event, ct) => call(event)(ct))

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
    * Retrieves an already send message from a text channel by it's id
    *
    * @param channelId the id of a text channel
    * @param messageId the id of the message
    * @return a rest action that will return the message
    */
  def retrieveMessage(channelId: String, messageId: String): RestAction[Message] = {
    Option(validJDA.getTextChannelById(channelId)) match {
      case Some(channel) => channel.getMessageById(messageId)
      case None =>
        Option(validJDA.getPrivateChannelById(channelId)) match {
          case Some(channel) => channel.getMessageById(messageId)
          case None => throw new IllegalArgumentException(s"TextChannel with id $channelId not found")
        }
    }
  }

  /**
    * Retrieves a text channel
    *
    * @param channelId the id of a text channel
    * @return Some text channel or None if no text channel with that id exists
    */
  def getTextChannel(channelId: String): Option[TextChannel] = Option(validJDA.getTextChannelById(channelId))

  /**
    * Sends a message to a text channel
    *
    * @param channelId the id of the text channel
    * @param chatMessage the actual message
    */
  def sendChatMessage(channelId: String, chatMessage: String): Unit = {
    Option(validJDA.getTextChannelById(channelId)) match {
      case Some(channel) => channel.sendMessage(chatMessage).queue(null, defaultFailureHandler)
      case None => throw new IllegalArgumentException(s"TextChannel with id $channelId not found")
    }
  }

  /**
    * Sends a embed to a text channel
    *
    * @param channelId the id of the text channel
    * @param embed     the embed to send
    */
  def sendChatMessage(channelId: String, embed: MessageEmbed): Unit = {
    Option(validJDA.getTextChannelById(channelId)) match {
      case Some(channel) => channel.sendMessage(embed).queue(null, defaultFailureHandler)
      case None => throw new IllegalArgumentException(s"TextChannel with id $channelId not found")
    }
  }

  /**
    * Sends a message with an attachment to a text channel
    *
    * @param channelId the id of the text channel
    * @param file path of a file inside the data folder that should be send
    * @param message optional containing the actual message (or none)
    */
  def sendFile(channelId: String, file: String, message: Option[String] = None): Unit = {
    val fileName = file.substring(file.lastIndexOf(File.separator) + 1)
    val fileIn = fileSystemActor.??[Option[Array[Byte]]](3)(LoadBinaryFile(file))
    if (fileIn.isDefined && fileIn.get.isDefined) {
      Option(validJDA.getTextChannelById(channelId)) match {
        case Some(channel) =>
          message match {
            case Some(m) => channel.sendFile(fileIn.get.get, fileName, new MessageBuilder(m).build()).queue(null, defaultFailureHandler)
            case None => channel.sendFile(fileIn.get.get, fileName).queue(null, defaultFailureHandler)
          }
        case None => throw new IllegalArgumentException(s"TextChannel with id $channelId not found")
      }
    } else {
      logger warn s"Could not load file '$file'"
    }
  }
}