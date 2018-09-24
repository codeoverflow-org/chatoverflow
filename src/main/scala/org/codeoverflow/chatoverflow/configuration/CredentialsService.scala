package org.codeoverflow.chatoverflow.configuration

import java.io.{BufferedWriter, File, FileWriter}

import scala.collection.mutable

/**
  * The credential service works with all needed login information for running services.
  *
  * @param credentialsFilePath the file path of the credentials file
  * @param password            a optional password to encrypt the saved credentials
  */
class CredentialsService(val credentialsFilePath: String, password: Array[Char]) {
  private val credentials = mutable.Map[(String, String), Credentials]()

  /**
    * Loads the credentials form the credentials file and decrypts them.
    */
  def load(): Unit = {

    // Create file if non existent
    if (!new File(credentialsFilePath).exists()) {
      save()
    }

    val encrypted = scala.io.Source.fromFile(credentialsFilePath).getLines().mkString
    val decrypted = CryptoUtil.decrypt(password, encrypted)
    val xmlContent = xml.Utility.trim(xml.XML.loadString(decrypted.get)) // TODO: Maybe, check for wrong password...

    credentials.clear()

    for (node <- xmlContent \ "entry") {
      val credentialsType = (node \ "type").text
      val credentialsIdentifier = (node \ "identifier").text

      val entry = new Credentials(credentialsIdentifier)
      entry.fromXML(node \ "values")

      credentials += (credentialsType, credentialsIdentifier) -> entry
    }
  }

  /**
    * Encrypts the credentials and saves them to the credentials file.
    */
  def save(): Unit = {

    val xmlContent =
      <credentials>
        {for (entry <- credentials.toList) yield {
        <entry>
          <type>
            {entry._1._1}
          </type>
          <identifier>
            {entry._1._2}
          </identifier>{entry._2.toXML}
        </entry>
      }}
      </credentials>

    // Encrypt and save
    val encrypted = CryptoUtil.encrypt(password, xmlContent.toString())

    val file = new File(credentialsFilePath)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(encrypted)
    bw.close()

  }

  def getAllCredentials(): Seq[((String, String), Credentials)] =
    credentials.toSeq

  def getCredentials(credentialsType: String, credentialsIdentifier: String): Option[Credentials] =
    credentials.get((credentialsType, credentialsIdentifier))

  def addCredentials(credentialsType: String, credentials: Credentials): Unit =
    this.credentials += (credentialsType, credentials.credentialsIdentifier) -> credentials

  def removeCredentials(credentialsType: String, credentialsIdentifier: String): Unit =
    credentials -= ((credentialsType, credentialsIdentifier))

  def existCredentials(credentialsType: String, credentialsIdentifier: String): Boolean =
    credentials.get((credentialsType, credentialsIdentifier)).isDefined

}