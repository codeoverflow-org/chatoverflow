package org.codeoverflow.chatoverflow2

import org.apache.log4j.Logger

abstract class WithLogger {
  protected val logger: Logger = Logger.getLogger(this.getClass)
}
