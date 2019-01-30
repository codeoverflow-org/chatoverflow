package org.codeoverflow.chatoverflow.ui.web

import org.codeoverflow.chatoverflow.Launcher
import org.codeoverflow.chatoverflow.ChatOverflow

trait WithChatOverflow {
  private[web] lazy val chatOverflow = Launcher.chatOverflow.get
}
