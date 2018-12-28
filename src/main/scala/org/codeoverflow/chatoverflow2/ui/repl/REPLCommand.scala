package org.codeoverflow.chatoverflow2.ui.repl

case class REPLCommand(methodToCall: Unit => Unit, description: String)
