package org.codeoverflow.chatoverflow.service.twitch.chat

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.configuration.Credentials
import org.codeoverflow.chatoverflow.service.Connector
import org.pircbotx.cap.EnableCapHandler
import org.pircbotx.hooks.events.{MessageEvent, UnknownEvent}
import org.pircbotx.{Configuration, PircBotX}

/**
  * The twitch connector connects to the irc service to work with chat messages.
  *
  * @param sourceIdentifier the name to the twitch account
  * @param credentials      the credentials to log into the irc chat
  */
class TwitchChatConnector(override val sourceIdentifier: String, credentials: Credentials) extends Connector(sourceIdentifier, credentials) {
  private val logger = Logger.getLogger(this.getClass)
  private val twitchChatListener = new TwitchChatListener
  private var bot: PircBotX = _
  private var running = false
  private var currentChannel: String = _

  def addMessageEventListener(listener: MessageEvent => Unit): Unit = {
    twitchChatListener.addMessageEventListener(listener)
  }

  def addUnknownEventListener(listener: UnknownEvent => Unit): Unit = {
    twitchChatListener.addUnknownEventListener(listener)
  }

  override def isRunning: Boolean = running

  override def init(): Unit = {
    if (!running) {
      logger info s"Starting connector for source '$sourceIdentifier' of type '$getUniqueTypeString'."

      bot = new PircBotX(getConfig)
      startBot()
      running = true
      logger info "Started connector."
    }
  }

  private def getConfig: Configuration = {

    val password = credentials.getValue(TwitchChatConnector.credentialsOauthKey)

    if (password.isEmpty) {
      logger warn s"key '${TwitchChatConnector.credentialsOauthKey}' not found in credentials for '$sourceIdentifier'."
    }

    setCurrentChannel(sourceIdentifier)

    new Configuration.Builder()
      .setAutoNickChange(false)
      .setOnJoinWhoEnabled(false)
      .setCapEnabled(true)
      .addCapHandler(new EnableCapHandler("twitch.tv/membership"))
      .addCapHandler(new EnableCapHandler("twitch.tv/tags"))
      .addServer("irc.chat.twitch.tv")
      .setName(credentials.credentialsIdentifier)
      .setServerPassword(password.get)
      .addAutoJoinChannel(currentChannel)
      .addListener(twitchChatListener)
      .buildConfiguration()

  }

  private def setCurrentChannel(channel: String): Unit = {
    if (channel.startsWith("#")) {
      currentChannel = channel.toLowerCase
    } else {
      currentChannel = "#" + channel.toLowerCase
    }
  }

  private def startBot(): Unit = {

    new Thread(() => {
      bot.startBot()
    }).start()

    while (bot.getState != PircBotX.State.CONNECTED) {
      logger info "Waiting while the bot is connecting..."
      Thread.sleep(100)
    }

  }

  override def getUniqueTypeString: String = this.getClass.getName

  def setChannel(channel: String): Unit = {
    // Todo: Leave channel
    setCurrentChannel(channel)
    bot.send().joinChannel(currentChannel)
    // TODO: TEST!
  }

  override def shutdown(): Unit = {
    bot.sendIRC().quitServer()
    bot.close()
    logger info s"Stopped connector for source '$sourceIdentifier' of type '$getUniqueTypeString'."
  }

  def sendChatMessage(chatMessage: String): Unit = bot.send().message(currentChannel, chatMessage)
}

object TwitchChatConnector {
  val credentialsOauthKey = "oauth"
}