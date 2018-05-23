package org.codeoverflow.chatoverflow.io.connector

import org.codeoverflow.chatoverflow.io.connector.ConnectorType.ConnectorType
import org.pircbotx.cap.EnableCapHandler
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.{MessageEvent, UnknownEvent}
import org.pircbotx.{Configuration, PircBotX}

import scala.collection.mutable.ListBuffer

class TwitchListener extends ListenerAdapter {
  private val messageEventListener = ListBuffer[MessageEvent => Unit]()
  private val unknownEventListener = ListBuffer[UnknownEvent => Unit]()

  override def onMessage(event: MessageEvent): Unit = {
    messageEventListener.foreach(listener => listener(event))
  }

  override def onUnknown(event: UnknownEvent): Unit = {
    unknownEventListener.foreach(listener => listener(event))
  }

  def addMessageEventListener(listener: MessageEvent => Unit): Unit = {
    messageEventListener += listener
  }

  def addUnknownEventListener(listener: UnknownEvent => Unit): Unit = {
    unknownEventListener += listener
  }
}

class TwitchConnector(channelName: String, credentials: TwitchCredentials) extends Connector(channelName) {

  private val bot = new PircBotX(getConfig)

  private val twitchListener: TwitchListener = new TwitchListener

  startBotAsync()

  private def getConfig: Configuration = {
    new Configuration.Builder()
      .setAutoNickChange(false)
      .setOnJoinWhoEnabled(false)
      .setCapEnabled(true)
      .addCapHandler(new EnableCapHandler("twitch.tv/membership"))
      .addCapHandler(new EnableCapHandler("twitch.tv/tags"))
      .addServer("irc.chat.twitch.tv")
      .setName(credentials.name)
      .setServerPassword(credentials.password)
      .addAutoJoinChannel({
        if (!channelName.startsWith("#"))
          "#" + channelName
        else
          channelName
      })
      .addListener(twitchListener)
      .buildConfiguration()
  }

  override def getType: ConnectorType = TwitchConnector.getType

  private def startBotAsync(): Unit = {
    new Thread(() => bot.startBot()).start()
  }

  def addMessageEventListener(listener: MessageEvent => Unit): Unit = {
    twitchListener.addMessageEventListener(listener)
  }

  def addUnknownEventListener(listener: UnknownEvent => Unit): Unit = {
    twitchListener.addUnknownEventListener(listener)
  }
}

object TwitchConnector {
  def getType: ConnectorType = ConnectorType.Twitch
}
