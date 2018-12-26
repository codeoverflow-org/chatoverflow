package org.codeoverflow.chatoverflow2.requirement.parameter

import org.codeoverflow.chatoverflow.api.io.parameter.StringParameter
import org.codeoverflow.chatoverflow2.registry.Impl

@Impl(impl = classOf[StringParameter])
class StringParameterImpl extends StringParameter {
  private var value = ""

  override def getType: Class[String] = classOf[String]

  override def serialize(): String = get()

  override def get(): String = value

  override def deserialize(value: String): Unit = set(value)

  override def set(value: String): Unit = this.value = value
}
