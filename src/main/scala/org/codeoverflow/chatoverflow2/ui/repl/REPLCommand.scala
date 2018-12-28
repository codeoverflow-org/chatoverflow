package org.codeoverflow.chatoverflow2.ui.repl

/**
  * A REPL Command hols a method that can be executed an a description.
  *
  * @param methodToCall the method to call, when the command is executed
  * @param description  the description to show when the user asks for help
  */
case class REPLCommand(methodToCall: Unit => Unit, description: String)
