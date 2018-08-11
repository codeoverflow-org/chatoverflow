package org.codeoverflow.chatoverflow.configuration

import scala.collection.mutable

/**
  * A credentials object encapsulates all login information for one service. Multiple information sets can be added.
  *
  * @param credentialsIdentifier the identifier of the source the credentials are working with
  */
class Credentials(val credentialsIdentifier: String) {
  private val values = mutable.Map[String, String]()

  def addValue(key: String, value: String): Unit = values += key -> value

  def getValue(key: String): Option[String] = values.get(key)

  def exists(key: String): Boolean = values.get(key).isDefined

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

  def fromXML(nodes: xml.NodeSeq): Unit = {
    for (node <- nodes \ "_") {
      val key = (node \\ "key").text
      val value = (node \\ "value").text

      values += key -> value
    }
  }
}