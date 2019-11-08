package org.codeoverflow.chatoverflow.requirement.parameter

import org.codeoverflow.chatoverflow.api.io.parameter.BooleanParameter
import org.codeoverflow.chatoverflow.registry.Impl
import java.lang.Boolean

/**
  * A parameter holding a boolean value.
  */
@Impl(impl = classOf[BooleanParameter])
class BooleanParameterImpl extends BooleanParameter {
  private var value: Boolean = null

  override def getType: Class[Boolean] = classOf[Boolean]

  override def serialize(): String = get().toString

  override def get(): Boolean = value

  override def deserialize(value: String): Unit = {
    set(Boolean.parseBoolean(value))
  }

  override def set(value: Boolean): Unit = this.value = value

}
