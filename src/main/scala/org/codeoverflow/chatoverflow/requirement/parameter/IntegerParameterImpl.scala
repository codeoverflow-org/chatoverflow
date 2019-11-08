package org.codeoverflow.chatoverflow.requirement.parameter

import org.codeoverflow.chatoverflow.api.io.parameter.IntegerParameter
import org.codeoverflow.chatoverflow.registry.Impl

/**
  * A parameter holding a int value.
  */
@Impl(impl = classOf[IntegerParameter])
class IntegerParameterImpl extends IntegerParameter {
  private var value: Integer = null

  override def getType: Class[Integer] = classOf[Integer]

  override def serialize(): String = get().toString

  override def get(): Integer = value

  override def deserialize(value: String): Unit = {
    try {
      set(Integer.valueOf(value))
    }
  }

  override def set(value: Integer): Unit = this.value = value

}
