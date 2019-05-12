package org.codeoverflow.chatoverflow.requirement.service.ftp.impl

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.output.chat.ChatOutput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.Connection
import org.codeoverflow.chatoverflow.requirement.service.ftp.FtpConnector

@Impl(impl = classOf[ChatOutput], connector = classOf[FtpConnector])
class FtpChatOutputImpl extends Connection[FtpConnector] with ChatOutput with WithLogger {
  override def init(): Boolean = {
    if (sourceConnector.isDefined) {


      sourceConnector.get.init()
    } else {
      logger warn "Source connector not set."
      false
    }
  }
}
