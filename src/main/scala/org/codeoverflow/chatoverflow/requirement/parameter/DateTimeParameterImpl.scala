package org.codeoverflow.chatoverflow.requirement.parameter

import java.time.{LocalDateTime, LocalTime}
import java.time.format.DateTimeFormatter

import org.codeoverflow.chatoverflow.api.io.parameter.{DateTimeParameter, TimeParameter}
/**
  * A parameter holding a LocalDateTime value.
  * Formatting is done in the ISO-8601 standard.
  * Example: `2011-12-30T10:15:30`
  */
class DateTimeParameterImpl extends DateTimeParameter {

  private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME
  private var value: LocalDateTime = _

  override def get(): LocalDateTime = value

  override def getType: Class[LocalDateTime] = classOf[LocalDateTime]

  override def serialize(): String = value.format(FORMATTER)

  override def deserialize(value: String): Unit = set(LocalDateTime.parse(value, FORMATTER))

  override def set(value: LocalDateTime): Unit = this.value = value
}
