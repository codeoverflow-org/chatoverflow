package org.codeoverflow.chatoverflow.requirement.parameter

import org.codeoverflow.chatoverflow.api.io.parameter.MapParameter
import org.codeoverflow.chatoverflow.registry.Impl

import scala.jdk.CollectionConverters._

/**
  * A parameter holding a Map<String, String> value.
  */
@Impl(impl = classOf[MapParameter])
class MapParameterImpl extends MapParameter {
  private var value: Map[String, String] = _

  override def getType: Class[java.util.Map[String, String]] = classOf[java.util.Map[String, String]]

  override def serialize(): String = value.map({ case (key, value) => s"($key;$value)" }).mkString(",")

  override def get(): java.util.Map[String, String] = value.asJava

  override def deserialize(value: String): Unit = {
    val kvpair = "\\((.+);(.+)\\)".r
    val konly = "\\((.+);\\)".r
    this.value = value.split(",")
      .map({ case kvpair(k, v) => k -> v; case konly(k) => k -> ""; case _ => throw new IllegalArgumentException("Could not convert String to Map")})
      .toMap
  }

  override def set(value: java.util.Map[String, String]): Unit = this.value = value.asScala.toMap

}
