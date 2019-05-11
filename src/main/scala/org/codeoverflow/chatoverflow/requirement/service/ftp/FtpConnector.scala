package org.codeoverflow.chatoverflow.requirement.service.ftp

import java.net.InetAddress

import akka.stream.alpakka.ftp.{FtpCredentials, FtpFileSettings, FtpSettings, FtpsSettings, SftpIdentity, SftpSettings}
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector

import scala.util.matching.Regex.Match

/**
  * The ftp connector connects to a ftp server to store or retrieve files.
  *
  * @param sourceIdentifier the unique source identifier (e.g. a login name), the connector should work with
  */

class FtpConnector(sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {
  private val username = "username"
  private val password = "password"
  private val sshKey = "sshKeyString"
  private val sshKeyPassphrase = "sshKeyPassword"
  override protected var requiredCredentialKeys = List()
  override protected var optionalCredentialKeys = List(username, password, sshKey, sshKeyPassphrase)

  val ftpProtocol = "ftp"
  val ftpDomain = ""
  val ftpPort = 21

  private def parseSourceIdentifier(sourceIdentifier: String): Boolean = {
    try {
      val ftpSplitRegex = "^(ftp|ftps|sftp)://([a-zA-Z]+[.a-zA-Z]+):([0-9]+)$".r
      ftpSplitRegex(ftpProtocol, ftpDomain, ftpPort) = sourceIdentifier
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
      if (credentials.get.getValue(username).get == "" || credentials.get.getValue(username).get == "anonymous") false {
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
        val ftpFileSettings = FtpSettings.apply(InetAddress.getByName(ftpDomain))
          .withPort(ftpPort)
          .withBinary(true)
          .withPassiveMode(true)
          .withCredentials(ftpCreds)

      } else if (ftpProtocol == "ftps") {
        val ftpFileSettings = FtpsSettings.apply(InetAddress.getByName(ftpDomain))
          .withPort(ftpPort)
          .withBinary(true)
          .withPassiveMode(true)
          .withCredentials(ftpCreds)

      } else if (ftpProtocol == "sftp") {
        if (credentials.get.exists(sshKey)) {
          if (credentials.get.exists(sshKeyPassphrase)) {
            val sshCreds = SftpIdentity.createRawSftpIdentity(sshKey.getBytes(), sshKeyPassphrase.getBytes())
            val ftpFileSettings = SftpSettings.apply(InetAddress.getByName(ftpDomain))
              .withPort(ftpPort)
              .withCredentials(ftpCreds)
              .withSftpIdentity(sshCreds)
          } else {
            val sshCreds = SftpIdentity.createRawSftpIdentity(sshKey.getBytes())
            val ftpFileSettings = SftpSettings.apply(InetAddress.getByName(ftpDomain))
              .withPort(ftpPort)
              .withCredentials(ftpCreds)
              .withSftpIdentity(sshCreds)
          }
        } else {
          val ftpFileSettings = SftpSettings.apply(InetAddress.getByName(ftpDomain))
            .withPort(ftpPort)
            .withCredentials(ftpCreds)
        }
      }
    }
    true
  }

  override def stop(): Boolean = {
    true
  }
}