package org.codeoverflow.chatoverflow.requirement.service.mockup

import org.codeoverflow.chatoverflow.WithLogger

import scala.collection.mutable.ListBuffer
import scala.util.Random
import scala.util.parsing.combinator.JavaTokenParsers

/**
  * A mockUp element is a token of the mockUp chat format.
  */
private[mockup] trait MockupElement

/**
  * A chat element represents a chat message.
  *
  * @param user      the username of the message
  * @param message   the message of the chat entry
  * @param isPremium true if the user has premium features
  */
private[mockup] case class ChatElement(user: String, message: String, isPremium: Boolean) extends MockupElement

/**
  * A delay element represents additional delay information.
  *
  * @param delay The delay between to messages in milliseconds
  */
private[mockup] case class DelayElement(delay: Int) extends MockupElement

/**
  * A repeat element represents the repetition of a segment of chat messages
  *
  * @param times the number of recursions
  */
private[mockup] case class RepeatElement(times: Int) extends MockupElement

/**
  * An empty element usually represents an empty line. It is the delimiter of a repetition segment.
  */
private[mockup] case class EmptyElement() extends MockupElement

/**
  * A ChatMessageParser is used to parse the textual representation of MockUpElements.
  *
  * @param defaultEscape        the default escape string for commands and comments
  * @param defaultSeparator     the default separator between username and message
  * @param defaultPremiumSymbol the premium symbol for user with premium features
  */
private[mockup] class ChatMessageParser(var defaultEscape: String = "!",
                                        var defaultSeparator: String = ":",
                                        var defaultPremiumSymbol: String = "*") extends JavaTokenParsers with WithLogger {

  /**
    * Random for random user-number generation.
    */
  private val random = new Random()

  /**
    * A message only (not starting with the escapeString and not containing a colon).
    */
  private val messageOnly: Parser[ChatElement] = (s"[^$defaultEscape][^$defaultSeparator]*".r | stringLiteral) ^^ {
    message => ChatElement("user%d".format(random.nextInt(1000)), message, isPremium = false)
  }

  /**
    * username and message, separated by colon
    */
  private val userAndMessage: Parser[ChatElement] =
    (s"[^$defaultPremiumSymbol$defaultEscape][^$defaultSeparator]*".r | stringLiteral) ~ ":".r ~ ".*".r ^^ {
      case user ~ _ ~ message => ChatElement(user, message, isPremium = false)
    }

  /**
    * Premium symbol, username and message
    */
  private val userAndMessagePremium: Parser[ChatElement] = s"$defaultPremiumSymbol" ~ userAndMessage ^^ {
    case _ ~ message => message.copy(isPremium = true)
  }

  /**
    * Chat element (all styles)
    */
  private val chatElement: Parser[ChatElement] = userAndMessagePremium | userAndMessage | messageOnly

  /**
    * Delay element (escaped)
    */
  private val delayElement: Parser[DelayElement] = s"$defaultEscape".r ~> "[\\d]+".r ^^ {
    delay => DelayElement(delay.toInt)
  }

  /**
    * Repeat element (escaped)
    */
  private val repeatElement: Parser[RepeatElement] = s"$defaultEscape".r ~> "[\\d]+".r <~ "[x]".r ^^ {
    times => RepeatElement(times.toInt)
  }

  /**
    * High-level element: A chat, delay or repeat element
    */
  private val element: Parser[MockupElement] = chatElement | repeatElement | delayElement

  /**
    * Takes the lines of a mockUp text file (e.g. provided by [[scala.io.Source.getLines]]) and generated mockup elements.
    *
    * @param lines All lines in proper mockUp Chat Format
    * @return A list of mockup elements
    */
  def parseMockUpFile(lines: Iterator[String]): List[MockupElement] = {

    val elementList = ListBuffer[MockupElement]()

    var counter = 0

    for (line: String <- lines) {

      if (line.isEmpty) {
        logger.info(s"Parsing... line $counter is empty.")
        elementList += EmptyElement()
      } else if (line.startsWith(s"$defaultEscape$defaultEscape")) {
        logger.info(s"Parsing... line $counter is a comment.")
      } else {
        logger.info(s"Parsing... line $counter.")
        elementList += parseMockUpLine(line).getOrElse(EmptyElement())
      }

      counter += 1
    }

    elementList.toList
  }

  /**
    * Parses a single line using the parser combinator.
    *
    * @param content the mockup line
    * @return A mockup element if there was no syntax error or None
    */
  private def parseMockUpLine(content: String): Option[MockupElement] = parseAll(element, content) match {
    case Success(result, _) =>
      logger.info("Parsed input successfully.")
      Some(result)
    case Error(msg, _) =>
      logger.error("Error while parsing: %s".format(msg))
      None
    case Failure(msg, _) =>
      logger.error("Failure while parsing: %s".format(msg))
      None
  }

}