package org.codeoverflow.chatoverflow.service.twitch

import org.apache.log4j.Logger
import org.codeoverflow.chatoverflow.service.{Connector, Credentials}
import org.pircbotx.cap.EnableCapHandler
import org.pircbotx.hooks.events.{MessageEvent, UnknownEvent}
import org.pircbotx.{Configuration, PircBotX}

class TwitchConnector(override val sourceIdentifier: String, credentials: Credentials) extends Connector(sourceIdentifier, credentials) {
  private val logger = Logger.getLogger(this.getClass)
  private val twitchChatListener = new TwitchChatListener
  private var bot: PircBotX = _
  private var running = false

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
      startBotAsync()
      running = true
      logger info "Started connector."
    }
  }

  private def getConfig: Configuration = {

    val password = credentials.getValue(TwitchConnector.credentialsOauthKey)

    if (password.isEmpty) {
      logger warn s"key '${TwitchConnector.credentialsOauthKey}' not found in credentials for '$sourceIdentifier'."
    }

    new Configuration.Builder()
      .setAutoNickChange(false)
      .setOnJoinWhoEnabled(false)
      .setCapEnabled(true)
      .addCapHandler(new EnableCapHandler("twitch.tv/membership"))
      .addCapHandler(new EnableCapHandler("twitch.tv/tags"))
      .addServer("irc.chat.twitch.tv")
      .setName(credentials.credentialsIdentifier)
      .setServerPassword(password.get)
      .addAutoJoinChannel({
        if (!sourceIdentifier.startsWith("#"))
          "#" + sourceIdentifier
        else
          sourceIdentifier
      })
      .addListener(twitchChatListener)
      .buildConfiguration()

  }

  private def startBotAsync(): Unit = {

    new Thread(() => {
      bot.startBot()
    }).start()

  }

  override def shutdown(): Unit = {
    bot.sendIRC().quitServer()
    bot.close()
    logger info s"Stopped connector for source '$sourceIdentifier' of type '$getUniqueTypeString'."
  }

  override def getUniqueTypeString: String = this.getClass.getName

  def sendChatMessage(channelName: String, chatMessage: String): Unit = bot.send().message(channelName, chatMessage)
}

object TwitchConnector {
  val credentialsOauthKey = "oauth"
}