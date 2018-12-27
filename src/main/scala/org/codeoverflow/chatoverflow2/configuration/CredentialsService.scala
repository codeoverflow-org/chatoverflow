package org.codeoverflow.chatoverflow2.configuration

import java.io.{BufferedWriter, File, FileWriter}

import org.codeoverflow.chatoverflow2.WithLogger

import scala.collection.mutable

/**
  * The credential service works with all needed login information for running services.
  *
  * @param credentialsFilePath the file path of the credentials file
  */
class CredentialsService(val credentialsFilePath: String) extends WithLogger {
  private val credentials = mutable.Map[(String, String), Credentials]()
  private var password = Array[Char]()

  /**
    * Sets the password. Needed to load or save credentials.
    *
    * @param password an array of chars, representing the users password
    */
  def setPassword(password: Array[Char]): Unit = this.password = password

  /**
    * Loads the credentials form the credentials file and decrypts them.
    *
    * @return true, if the loading process was successful
    */
  def load(): Boolean = {

    if (password.length == 0) {
      logger error "Password was not specified. Unable to load credentials."
      false
    } else {

      // Create file if non existent
      if (!new File(credentialsFilePath).exists()) {
        save()
      }

      try {
        val encrypted = scala.io.Source.fromFile(credentialsFilePath).getLines().mkString
        val decrypted = CryptoUtil.decrypt(password, encrypted)

        if (decrypted.isEmpty) {
          logger error "Wrong password. Unable to load credentials."
          false
        } else {
          logger info "Password correct."

          val xmlContent = xml.Utility.trim(xml.XML.loadString(decrypted.get))

          credentials.clear()

          for (node <- xmlContent \ "entry") {
            val credentialsType = (node \ "type").text
            val credentialsIdentifier = (node \ "identifier").text

            val entry = new Credentials(credentialsIdentifier)
            entry.fromXML(node \ "values")

            credentials += (credentialsType, credentialsIdentifier) -> entry
          }

          logger info "Finished loading credentials."
          true
        }
      } catch {
        case e: Exception =>
          logger error s"Unable to load credentials. An exception occurred: ${e.getMessage}"
          false
      }
    }
  }

  /**
    * Encrypts the credentials and saves them to the credentials file.
    *
    * @return true, if the saving process was successful
    */
  def save(): Boolean = {

    if (password.length == 0) {
      logger error "Password was not specified. Unable to save credentials."
      false
    } else {

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

      try {
        // Encrypt and save
        val encrypted = CryptoUtil.encrypt(password, xmlContent.toString())

        val file = new File(credentialsFilePath)
        val bw = new BufferedWriter(new FileWriter(file))
        bw.write(encrypted)
        bw.close()
        true
      } catch {
        case e: Exception =>
          logger error s"Unable to save credentials. An exception occurred: ${e.getMessage}"
          false
      }
    }
  }

  def get(qualifiedConnectorName: String, credentialsIdentifier: String): Option[Credentials] =
    credentials.get((qualifiedConnectorName, credentialsIdentifier))

  def add(qualifiedConnectorName: String, credentials: Credentials): Unit =
    this.credentials += (qualifiedConnectorName, credentials.credentialsIdentifier) -> credentials

  def remove(qualifiedConnectorName: String, credentialsIdentifier: String): Unit =
    credentials -= ((qualifiedConnectorName, credentialsIdentifier))

  def exist(qualifiedConnectorName: String, credentialsIdentifier: String): Boolean =
    credentials.get((qualifiedConnectorName, credentialsIdentifier)).isDefined

}