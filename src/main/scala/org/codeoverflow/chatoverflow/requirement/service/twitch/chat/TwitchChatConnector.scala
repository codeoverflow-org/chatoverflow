package org.codeoverflow.chatoverflow.requirement.service.twitch.chat

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.EventConnector
import org.pircbotx.cap.EnableCapHandler
import org.pircbotx.{Configuration, PircBotX}

import scala.collection.mutable.ListBuffer

/**
  * The twitch connector connects to the irc service to work with chat messages.
  *
  * @param sourceIdentifier the name to the twitch account
  */
class TwitchChatConnector(override val sourceIdentifier: String) extends EventConnector(sourceIdentifier) with WithLogger {
  private val twitchChatListener = new TwitchChatListener
  private val connectionListener = new TwitchChatConnectListener(onConnect)
  private val oauthKey = "oauth"
  override protected var requiredCredentialKeys: List[String] = List(oauthKey)
  override protected var optionalCredentialKeys: List[String] = List()
  private var bot: PircBotX = _
  private var status: Option[(Boolean, String)] = None
  private val channels = ListBuffer[String]()

  twitchChatListener.registerEventHandler((event, ct) => call(event)(ct, ct)) // passes all events from the twitch chat to the in/outputs

  def joinChannel(channel: String): Unit = {
    bot.send().joinChannel(channel)
    channels += channel
  }

  def sendChatMessage(channel: String, chatMessage: String): Unit = {
    if (!isJoined(channel)) throw new IllegalArgumentException(s"you must join the '$channel' channel, before you can send messages to it")
    bot.send().message(channel, chatMessage)
  }

  override def getUniqueTypeString: String = this.getClass.getName

  def isJoined(channel: String): Boolean = channels.contains(channel)

  def getUsername: String = bot.getNick

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
        .addListener(connectionListener)
        .setAutoReconnect(true)
        .buildConfiguration()
    } else {
      logger error "No credentials set!"
      new Configuration.Builder().buildConfiguration()
    }

  }

  /**
    * Gets called by the TwitchChatConnectListener when the bot has connected.
    * Saves the passed information into the status variable.
    */
  private def onConnect(success: Boolean, msg: String): Unit = {
    status.synchronized {
      // tell the thread which starts the connector that the status has been reported
      status.notify()
      status = Some((success, msg))
    }
  }

  /**
    * Starts the connector, e.g. creates a connection with its platform.
    */
  override def start(): Boolean = {
    bot = new PircBotX(getConfig)
    startBot()
  }

  private def startBot(): Boolean = {
    new Thread(() => {
      bot.startBot()
    }).start()

    logger info "Waiting while the bot is connecting and logging in..."
    status.synchronized {
      status.wait(10000)
    }

    if (status.isEmpty) {
      logger error "Bot couldn't connect within timeout of 10 seconds."
      return false
    }

    val (success, msg) = status.get
    if (!success) {
      logger error s"Bot couldn't connect. Reason: $msg."
    }

    success
  }

  /**
    * This stops the activity of the connector, e.g. by closing the platform connection.
    */
  override def stop(): Boolean = {
    if (bot.isConnected) {
      bot.sendIRC().quitServer()
      bot.close()
    }
    status = None
    channels.clear()
    true
  }
}

object TwitchChatConnector {

  /**
    * Ensures that the channel is in following format: "#lowercasename"
    *
    * @param chan the unmodified channel
    * @return the channel in the correct format, changes nothing if already correct
    */
  private[chat] def formatChannel(chan: String): String = s"#${chan.stripPrefix("#").toLowerCase}"
}