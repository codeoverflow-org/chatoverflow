package org.codeoverflow.chatoverflow.service

abstract class Connector(val sourceIdentifier: String, credentials: Credentials) {

  def getUniqueTypeString: String

  def isRunning: Boolean

  def init(): Unit

  def shutdown(): Unit
}