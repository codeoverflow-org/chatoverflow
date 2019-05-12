package org.codeoverflow.chatoverflow.requirement.service.ftp

import java.net.InetAddress

import akka.stream.IOResult
import akka.stream.alpakka.ftp.{FtpCredentials, FtpSettings, FtpsSettings, RemoteFileSettings, SftpIdentity, SftpSettings}
import akka.stream.scaladsl.{Framing, Source}
import akka.util.ByteString
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector
import org.codeoverflow.chatoverflow.connector.actor.ftp.{FileReadActor, ReadFtp, ReadFtps, ReadSftp}

import scala.concurrent.Future
import scala.util.matching.Regex

/**
  * The ftp connector connects to a ftp server to store or retrieve files.
  *
  * @param sourceIdentifier the unique source identifier (e.g. a login name), the connector should work with
  */

class FtpConnector(sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {
  private val fileReadActor = createActor[FileReadActor]()
  protected val timeout = 10
  private val username = "username"
  private val password = "password"
  private val sshKey = "sshKeyString"
  private val sshKeyPassphrase = "sshKeyPassword"
  override protected var requiredCredentialKeys = List[String]()
  override protected var optionalCredentialKeys = List(username, password, sshKey, sshKeyPassphrase)

  var ftpProtocol = "ftp"
  var ftpDomain = ""
  var ftpPort = 21

  var ftpFileSettings: RemoteFileSettings = _

  private def parseSourceIdentifier(sourceIdentifier: String): Boolean = {
    try {
      val ftpSplitRegex = new Regex("^(ftp|ftps|sftp)://([a-zA-Z]+[.a-zA-Z]+):([0-9]+)$")
      val result = ftpSplitRegex.findFirstMatchIn(sourceIdentifier).get
      ftpProtocol = result.group(0)
      ftpDomain = result.group(1)
      ftpPort = result.group(2).toInt

      true
    } catch {
      case e: Exception => {
        logger error s"$e"
        false
      }
    }
  }

  override def start(): Boolean = {
    parseSourceIdentifier(sourceIdentifier)
    var ftpUsername = "anonymous"
    var ftpPassword = ""

    if (credentials.get.exists(username)) {
      if (!(credentials.get.getValue(username).get == "" || credentials.get.getValue(username).get == "anonymous")) {
        ftpUsername = credentials.get.getValue(username).get
      }

      if (credentials.get.exists(password)) {
        ftpPassword = credentials.get.getValue(password).get
      }


      if (credentials.get.exists(password)) {
        ftpPassword = credentials.get.getValue(password).get
      }

      val ftpCreds = FtpCredentials.create(ftpUsername, ftpPassword)

      if (ftpProtocol == "ftp") {
        ftpFileSettings = FtpSettings.apply(InetAddress.getByName(ftpDomain))
          .withPort(ftpPort)
          .withBinary(true)
          .withPassiveMode(true)
          .withCredentials(ftpCreds)

      } else if (ftpProtocol == "ftps") {
        ftpFileSettings = FtpsSettings.apply(InetAddress.getByName(ftpDomain))
          .withPort(ftpPort)
          .withBinary(true)
          .withPassiveMode(true)
          .withCredentials(ftpCreds)

      } else if (ftpProtocol == "sftp") {
        if (credentials.get.exists(sshKey)) {
          if (credentials.get.exists(sshKeyPassphrase)) {
            val sshCreds = SftpIdentity.createRawSftpIdentity(sshKey.getBytes(), sshKeyPassphrase.getBytes())
            ftpFileSettings = SftpSettings.apply(InetAddress.getByName(ftpDomain))
              .withPort(ftpPort)
              .withCredentials(ftpCreds)
              .withSftpIdentity(sshCreds)
          } else {
            val sshCreds = SftpIdentity.createRawSftpIdentity(sshKey.getBytes())
            ftpFileSettings = SftpSettings.apply(InetAddress.getByName(ftpDomain))
              .withPort(ftpPort)
              .withCredentials(ftpCreds)
              .withSftpIdentity(sshCreds)
          }
        } else {
          ftpFileSettings = SftpSettings.apply(InetAddress.getByName(ftpDomain))
            .withPort(ftpPort)
            .withCredentials(ftpCreds)
        }
      }
    }
    true
  }


  def retrieveFilecontentFromPath(path: String, settings: Any): String = {
    val ftpSource =
      ftpProtocol match {
        case "ftp" => askActor(fileReadActor, timeout, ReadFtp(path, settings.asInstanceOf[FtpSettings]))
        case "sftp" => askActor(fileReadActor, timeout, ReadSftp(path, settings.asInstanceOf[SftpSettings]))
        case "ftps" => askActor(fileReadActor, timeout, ReadFtps(path, settings.asInstanceOf[FtpsSettings]))
      }

    val splitter = Framing.delimiter(
      ByteString(""),
      maximumFrameLength = Int.MaxValue,
      allowTruncation = false
    )

    val result: Source[ByteString, Future[IOResult]] = ftpSource.via(splitter)
  }

  override def stop(): Boolean = {
    true
  }
}