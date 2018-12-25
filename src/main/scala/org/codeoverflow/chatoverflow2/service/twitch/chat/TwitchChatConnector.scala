package org.codeoverflow.chatoverflow2.service.twitch.chat

import org.codeoverflow.chatoverflow.configuration.Credentials
import org.codeoverflow.chatoverflow2.WithLogger
import org.codeoverflow.chatoverflow2.requirement.Connector
import org.pircbotx.cap.EnableCapHandler
import org.pircbotx.hooks.events.{MessageEvent, UnknownEvent}
import org.pircbotx.{Configuration, PircBotX}

/**
  * The twitch connector connects to the irc service to work with chat messages.
  *
  * @param sourceIdentifier the name to the twitch account
  */
class TwitchChatConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {
  private val twitchChatListener = new TwitchChatListener
  private var credentials: Option[Credentials] = None
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

    if (credentials.isDefined) {

      val password = credentials.get.getValue(TwitchChatConnector.credentialsOauthKey)

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
        .setName(credentials.get.credentialsIdentifier)
        .setServerPassword(password.getOrElse(""))
        .addAutoJoinChannel(currentChannel)
        .addListener(twitchChatListener)
        .buildConfiguration()
    } else {
      logger error "No credentials set!"
      new Configuration.Builder().buildConfiguration()
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

  def setChannel(channel: String): Unit = {
    // Todo: Leave channel
    setCurrentChannel(channel)
    bot.send().joinChannel(currentChannel)
    // TODO: TEST!
  }

  private def setCurrentChannel(channel: String): Unit = {
    if (channel.startsWith("#")) {
      currentChannel = channel.toLowerCase
    } else {
      currentChannel = "#" + channel.toLowerCase
    }
  }

  override def shutdown(): Unit = {
    bot.sendIRC().quitServer()
    bot.close()
    logger info s"Stopped connector for source '$sourceIdentifier' of type '$getUniqueTypeString'."
  }

  override def getUniqueTypeString: String = this.getClass.getName

  def sendChatMessage(chatMessage: String): Unit = bot.send().message(currentChannel, chatMessage)

  /**
    * Sets the credentials needed for login or authentication of the connector to its platform
    *
    * @param credentials the credentials object to login to the platform
    */
  override def setCredentials(credentials: Credentials): Unit = this.credentials = Some(credentials)
}

object TwitchChatConnector {
  val credentialsOauthKey = "oauth"
}

// TODO: Custom credentials for every type with custom methods (which are a very abstract KV-access)