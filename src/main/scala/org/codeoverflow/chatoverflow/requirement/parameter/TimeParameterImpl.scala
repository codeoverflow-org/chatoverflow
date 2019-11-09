package org.codeoverflow.chatoverflow.requirement.parameter

import java.time.LocalTime
import java.time.format.DateTimeFormatter

import org.codeoverflow.chatoverflow.api.io.parameter.TimeParameter

/**
  * A parameter holding a LocalTime value.
  * Formatting is done in the ISO-8601 standard.
  * Example: `10:15:30`
  */
class TimeParameterImpl extends TimeParameter {

  private val FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME
  private var value: LocalTime = _

  override def get(): LocalTime = value

  override def getType: Class[LocalTime] = classOf[LocalTime]

  override def serialize(): String = value.format(FORMATTER)

  override def deserialize(value: String): Unit = set(LocalTime.parse(value, FORMATTER))

  override def set(value: LocalTime): Unit = this.value = value
}
