package org.codeoverflow.chatoverflow

import org.apache.log4j.Logger

/**
  * This trait is just syntactical sugar to provide an easy way to use a logger.
  */
trait WithLogger {
  /**
    * This logger can be used to print status updates.
    */
  protected val logger: Logger = Logger.getLogger(this.getClass)
}
