package org.codeoverflow.chatoverflow.service

abstract class Connector() {

  def getUniqueTypeString: String

  def isRunning: Boolean

  def needsCredentials: Boolean

  def setCredentials(credentials: Credentials)

  def init(sourceId: String): Unit

  def shutdown(): Unit
}