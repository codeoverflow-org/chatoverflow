package org.codeoverflow.chatoverflow.configuration

import java.io.{BufferedWriter, File, FileWriter}

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.ConnectorRegistry

import scala.collection.mutable

/**
  * The credential service works with all needed login information for running services.
  *
  * @param credentialsFilePath the file path of the credentials file
  */
class CredentialsService(val credentialsFilePath: String) extends WithLogger {
  private[this] var password = Array[Char]()

  /**
    * Sets the password. Needed to load or save credentials.
    *
    * @param password an array of chars, representing the users password
    */
  def setPassword(password: Array[Char]): Unit = this.password = password

  /**
    * Returns true, if a password is set. Note that this says nothing about the correctness of it.
    *
    * @return true, if the password length is greater than zero
    */
  def isLoggedIn: Boolean = {
    password.length > 0
  }

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
        val encrypted = scala.io.Source.fromFile(credentialsFilePath)
        val decrypted = CryptoUtil.decrypt(password, encrypted.getLines().mkString)
        encrypted.close

        if (decrypted.isEmpty) {
          logger warn "Wrong password. Unable to load credentials."
          false
        } else {
          logger info "Password correct."

          val xmlContent = xml.Utility.trim(xml.XML.loadString(decrypted.get))

          for (node <- xmlContent \ "entry") {
            val credentialsType = (node \ "type").text
            val credentialsIdentifier = (node \ "identifier").text

            val entry = new Credentials(credentialsIdentifier)
            entry.fromXML(node \ "values")

            // Set credentials directly to the connector
            ConnectorRegistry.setConnectorCredentials(credentialsIdentifier, credentialsType, entry)
            logger info "Successfully set credentials for this connector."

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

      // Because credentials are not saved here, they have to be retrieved from every connector
      // (connectorType, connectorIdentifier) -> Credentials
      val credentials = mutable.Map[(String, String), Credentials]()

      for (key <- ConnectorRegistry.getConnectorKeys) {
        val connector = ConnectorRegistry.getConnector(key.sourceIdentifier, key.qualifiedConnectorType)

        if (connector.isEmpty) {
          // This should never happen
          logger warn s"Connector '${key.sourceIdentifier}' was not found, but created."
        } else {
          val retrievedCredentials = connector.get.getCredentials

          if (retrievedCredentials.isEmpty) {
            logger warn s"Credentials object for connector '${key.sourceIdentifier}' was empty."
          } else {
            credentials += (key.qualifiedConnectorType, key.sourceIdentifier) -> retrievedCredentials.get
          }
        }
      }

      logger info s"Found ${credentials.size} credential objects to save."

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

        if (!file.getParentFile.exists()) {
          file.getParentFile.mkdirs()
        }

        val bw = new BufferedWriter(new FileWriter(file))
        bw.write(encrypted)
        bw.close()
        logger info "Successfully saved credentials."
        true
      } catch {
        case e: Exception =>
          logger error s"Unable to save credentials. An exception occurred: ${e.getMessage}"
          false
      }
    }
  }
}