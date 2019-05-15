package org.codeoverflow.chatoverflow.requirement.service.twitch.chat

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector
import org.pircbotx.cap.EnableCapHandler
import org.pircbotx.hooks.events.{MessageEvent, UnknownEvent}
import org.pircbotx.{Configuration, PircBotX}

import scala.collection.mutable.ListBuffer

/**
  * The twitch connector connects to the irc service to work with chat messages.
  *
  * @param sourceIdentifier the name to the twitch account
  */
class TwitchChatConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {
  private val twitchChatListener = new TwitchChatListener
  private val oauthKey = "oauth"
  override protected var requiredCredentialKeys: List[String] = List(oauthKey)
  override protected var optionalCredentialKeys: List[String] = List()
  private var bot: PircBotX = _
  private val channels = ListBuffer[String]()

  def addMessageEventListener(listener: MessageEvent => Unit): Unit = {
    twitchChatListener.addMessageEventListener(listener)
  }

  def addUnknownEventListener(listener: UnknownEvent => Unit): Unit = {
    twitchChatListener.addUnknownEventListener(listener)
  }

  def joinChannel(channel: String): Unit = {
    bot.send().joinChannel(channel)
    channels += channel
  }

  def isJoined(channel: String): Boolean = channels.contains(channel)

  override def getUniqueTypeString: String = this.getClass.getName

  def sendChatMessage(channel: String, chatMessage: String): Unit = {
    if (!isJoined(channel)) throw new IllegalArgumentException(s"you must join the '$channel' channel, before you can send messages to it")
    bot.send().message(channel, chatMessage)
  }

  private def getConfig: Configuration = {

    if (credentials.isDefined) {

      val password = credentials.get.getValue(oauthKey)

      if (password.isEmpty) {
        logger warn s"key '$oauthKey' not found in credentials for '$sourceIdentifier'."
      }

      new Configuration.Builder()
        .setAutoNickChange(false)
        .setOnJoinWhoEnabled(false)
        .setCapEnabled(true)
        .addCapHandler(new EnableCapHandler("twitch.tv/membership"))
        .addCapHandler(new EnableCapHandler("twitch.tv/tags"))
        .addServer("irc.chat.twitch.tv")
        .setName(credentials.get.credentialsIdentifier)
        .setServerPassword(password.getOrElse(""))
        .addListener(twitchChatListener)
        .buildConfiguration()
    } else {
      logger error "No credentials set!"
      new Configuration.Builder().buildConfiguration()
    }

  }

  /**
    * Starts the connector, e.g. creates a connection with its platform.
    */
  override def start(): Boolean = {
    bot = new PircBotX(getConfig)
    startBot()
    true
  }

  private def startBot(): Unit = {

    var errorCount = 0

    new Thread(() => {
      bot.startBot()
    }).start()

    while (bot.getState != PircBotX.State.CONNECTED && errorCount < 30) {
      logger info "Waiting while the bot is connecting..."
      Thread.sleep(100)
      errorCount += 1
    }

    // TODO: Enable detection for wrong credentials / bot disconnect

    if (errorCount >= 30) {
      logger error "Fatal. Unable to start bot."
    }

  }

  /**
    * This stops the activity of the connector, e.g. by closing the platform connection.
    */
  override def stop(): Boolean = {
    bot.sendIRC().quitServer()
    bot.close()
    true
  }
}