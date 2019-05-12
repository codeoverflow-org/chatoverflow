package org.codeoverflow.chatoverflow.connector.actor.ftp

import akka.actor.Actor
import akka.stream.alpakka.ftp.{FtpSettings, FtpsSettings, SftpSettings}
import akka.stream.alpakka.ftp.scaladsl.{Ftp, Ftps, Sftp}
import akka.util.ByteString

class FileWriteActor extends Actor {
  override def receive: Receive = {
    case WriteFtp(path, settings, append, content) =>
      try {
        val ftpFileList = Ftp.toPath(path, settings, append)
        sender ! ftpFileList
      } catch {
        case _: Exception => None
      }
    case WriteFtps(path, settings, append, content) =>
      try {
        val ftpFileList = Ftps.toPath(path, settings, append)
        sender ! ftpFileList
      } catch {
        case _: Exception => None
      }
    case WriteSftp(path, settings, append, content) =>
      try {
        val ftpFileList = Sftp.toPath(path, settings, append)
        sender ! ftpFileList
      } catch {
        case _: Exception => None
      }
  }
}

/**
  * TODO: Documentation
  *
  * @param path
  * @param settings
  * @param append
  */

case class WriteFtp(path: String,
                    settings: FtpSettings,
                    append: Boolean,
                    content: ByteString)

case class WriteFtps(path: String,
                     settings: FtpsSettings,
                     append: Boolean,
                     content: ByteString)

case class WriteSftp(path: String,
                     settings: SftpSettings,
                     append: Boolean,
                     content: ByteString)
