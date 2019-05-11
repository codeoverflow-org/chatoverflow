package org.codeoverflow.chatoverflow.requirement.service.twitch.chat

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector
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
  private val oauthKey = "oauth"
  override protected var requiredCredentialKeys: List[String] = List(oauthKey)
  override protected var optionalCredentialKeys: List[String] = List()
  private var bot: PircBotX = _
  private var currentChannel: String = _

  def addMessageEventListener(listener: MessageEvent => Unit): Unit = {
    twitchChatListener.addMessageEventListener(listener)
  }

  def addUnknownEventListener(listener: UnknownEvent => Unit): Unit = {
    twitchChatListener.addUnknownEventListener(listener)
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

  override def getUniqueTypeString: String = this.getClass.getName

  def sendChatMessage(chatMessage: String): Unit = bot.send().message(currentChannel, chatMessage)

  private def getConfig: Configuration = {

    if (credentials.isDefined) {

      val password = credentials.get.getValue(oauthKey)

      if (password.isEmpty) {
        logger warn s"key '$oauthKey' not found in credentials for '$sourceIdentifier'."
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