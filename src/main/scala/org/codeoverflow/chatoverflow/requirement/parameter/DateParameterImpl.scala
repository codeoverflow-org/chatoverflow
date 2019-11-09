package org.codeoverflow.chatoverflow.requirement.parameter

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.codeoverflow.chatoverflow.api.io.parameter.DateParameter

/**
  * A parameter holding a LocalDate value.
  * Formatting is done in the ISO-8601 standard.
  * Example: `2011-12-30`
  */
class DateParameterImpl extends DateParameter {

  private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
  private var value: LocalDate = _

  override def get(): LocalDate = value

  override def getType: Class[LocalDate] = classOf[LocalDate]

  override def serialize(): String = value.format(FORMATTER)

  override def deserialize(value: String): Unit = set(LocalDate.parse(value, FORMATTER))

  override def set(value: LocalDate): Unit = this.value = value
}
