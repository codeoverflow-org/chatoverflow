package org.codeoverflow.chatoverflow.configuration

import scala.collection.mutable

/**
  * A credentials object encapsulates all login information for one service. Multiple information sets can be added.
  *
  * @param credentialsIdentifier the identifier of the source the credentials are working with
  */
class Credentials(val credentialsIdentifier: String) {
  private val values = mutable.Map[String, String]()

  /**
    * Add a entry to the credentials object.
    *
    * @param key   the key of the entry
    * @param value the value to store
    */
  def addValue(key: String, value: String): Unit = values += key -> value

  /**
    * Removes a entry from the credentials object.
    *
    * @param key the key to remove from the map
    */
  def removeValue(key: String): Unit = values -= key

  /**
    * Returns the value of an entry of the credentials object.
    *
    * @param key the key to get the corresponding value from a entry
    * @return the optional value
    */
  def getValue(key: String): Option[String] = values.get(key)

  def exists(key: String): Boolean = values.get(key).isDefined

  /**
    * Converts the credentials object to ready-to-save xml.
    *
    * @return
    */
  def toXML: xml.Node = {
    <values>
      {for (value <- values) yield {
      <entry>
        <key>
          {value._1}
        </key>
        <value>
          {value._2}
        </value>
      </entry>
    }}
    </values>
  }

  /**
    * Creates the credentials entries from  the given xml.
    *
    * @param nodes the xml in the right format
    */
  def fromXML(nodes: xml.NodeSeq): Unit = {
    for (node <- nodes \ "_") {
      val key = (node \\ "key").text
      val value = (node \\ "value").text

      values += key -> value
    }
  }
}