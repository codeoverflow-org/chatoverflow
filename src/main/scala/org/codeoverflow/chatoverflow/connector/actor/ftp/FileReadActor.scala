package org.codeoverflow.chatoverflow.connector.actor.ftp

import akka.actor.Actor
import akka.stream.alpakka.ftp.{FtpSettings, FtpsSettings, SftpSettings}
import akka.stream.alpakka.ftp.scaladsl.{Ftp, Ftps, Sftp}
import org.apache.commons.net.ftp.FTPClient

class FileReadActor extends Actor {
  override def receive: Receive = {
    case ReadFtp(path, settings) =>
      try {
        val ftpFile = Ftp.fromPath(path, settings)
        sender ! ftpFile
      } catch {
        case _: Exception => None
      }
    case ReadFtps(path, settings) =>
      try {
        val ftpFile = Ftps.fromPath(path, settings)
        sender ! ftpFile
      } catch {
        case _: Exception => None
      }
    case ReadSftp(path, settings) =>
      try {
        val ftpFile = Sftp.fromPath(path, settings)
        sender ! ftpFile
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
  */

case class ReadFtp(path: String,
                   settings: FtpSettings)

case class ReadFtps(path: String,
                    settings: FtpsSettings)

case class ReadSftp(path: String,
                    settings: SftpSettings)
