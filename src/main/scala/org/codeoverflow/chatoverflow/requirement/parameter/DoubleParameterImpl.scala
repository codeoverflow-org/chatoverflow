package org.codeoverflow.chatoverflow.requirement.parameter

import org.codeoverflow.chatoverflow.api.io.parameter.DoubleParameter
import org.codeoverflow.chatoverflow.registry.Impl
import java.lang.Double

@Impl(impl = classOf[DoubleParameter])
class DoubleParameterImpl extends DoubleParameter {

  private var value: Double = null

  override def getType: Class[Double] = classOf[Double]

  override def serialize(): String = get().toString

  override def get(): Double = value

  override def deserialize(value: String): Unit = {
    try {
      set(Double.valueOf(value))
    }
  }

  override def set(value: Double): Unit = this.value = value
}
