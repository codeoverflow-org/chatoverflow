package org.codeoverflow.chatoverflow.service

import org.codeoverflow.chatoverflow.configuration.Credentials

abstract class Connector(val sourceIdentifier: String, credentials: Credentials) {

  def getUniqueTypeString: String

  def isRunning: Boolean

  def init(): Unit

  def shutdown(): Unit
}