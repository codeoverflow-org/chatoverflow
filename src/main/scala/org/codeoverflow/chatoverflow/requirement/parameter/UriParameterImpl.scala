package org.codeoverflow.chatoverflow.requirement.parameter

import org.codeoverflow.chatoverflow.api.io.parameter.UriParameter
import org.codeoverflow.chatoverflow.registry.Impl
import java.net.URI

@Impl(impl = classOf[UriParameter])
class UriParameterImpl extends UriParameter {
  private var value: URI = null

  override def getType: Class[URI] = classOf[URI]

  override def serialize(): String = get().toString

  override def get(): URI = value

  override def deserialize(value: String): Unit = set(new URI(value))

  override def set(value: URI): Unit = this.value = value
}
