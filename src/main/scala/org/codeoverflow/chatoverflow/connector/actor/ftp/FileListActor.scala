package org.codeoverflow.chatoverflow.connector.actor.ftp

import akka.actor.Actor
import akka.stream.alpakka.ftp.{FtpSettings, FtpsSettings, SftpSettings}
import akka.stream.alpakka.ftp.scaladsl.{Ftp, Ftps, Sftp}

class FileListActor extends Actor {
  override def receive: Receive = {
    case ListFtp(path, settings) =>
      try {
        val ftpFileList = Ftp.ls(path, settings).map((ftpFileList, _))
        sender ! ftpFileList
      } catch {
        case _: Exception => None
      }
    case ListFtps(path, settings) =>
      try {
        val ftpFileList = Ftps.ls(path, settings).map((ftpFileList, _))
        sender ! ftpFileList
      } catch {
        case _: Exception => None
      }
    case ListSftp(path, settings) =>
      try {
        val ftpFileList = Sftp.ls(path, settings).map((ftpFileList, _))
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
  */

case class ListFtp(path: String,
                   settings: FtpSettings)

case class ListFtps(path: String,
                    settings: FtpsSettings)

case class ListSftp(path: String,
                    settings: SftpSettings)
