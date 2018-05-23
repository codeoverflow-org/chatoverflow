package org.codeoverflow.chatoverflow.framework

import org.codeoverflow.chatoverflow.api.plugin.PluginManager

class PluginManagerImpl extends PluginManager {
  override def getDummyMessage: String = "This is a message!"

  //override def getTwitchChatInput: TwitchChatInput = new TwitchChatInputImpl
}
