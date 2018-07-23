package org.codeoverflow.chatoverflow.config

import java.io.File

import org.codeoverflow.chatoverflow.service.Credentials

import scala.collection.mutable

class CredentialsService(val credentialsFilePath: String, password: Array[Char]) {
  private val credentials = mutable.Map[(String, String), Credentials]()

  def load(): Unit = {

    // Create file if non existent
    if (!new File(credentialsFilePath).exists()) {
      save()
    }

    // TODO: Decrypt
    val xmlContent = xml.Utility.trim(xml.XML.loadFile(credentialsFilePath))

    credentials.clear()

    for (node <- xmlContent \ "entry") {
      val credentialsType = (node \ "type").text
      val credentialsIdentifier = (node \ "identifier").text

      val entry = new Credentials(credentialsIdentifier)
      entry.fromXML(node \ "values")

      credentials += (credentialsType, credentialsIdentifier) -> entry
    }
  }

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

    // TODO: Encrypt
    xml.XML.save(credentialsFilePath, xmlContent)

  }

  def getCredentials(credentialsType: String, credentialsIdentifier: String): Option[Credentials] =
    credentials.get((credentialsType, credentialsIdentifier))

  def addCredentials(credentialsType: String, credentials: Credentials): Unit =
    this.credentials += (credentialsType, credentials.credentialsIdentifier) -> credentials

  def removeCredentials(credentialsType: String, credentialsIdentifier: String): Unit =
    credentials -= ((credentialsType, credentialsIdentifier))

}