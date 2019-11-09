package org.codeoverflow.chatoverflow.requirement.parameter

import collection.JavaConverters._
import org.codeoverflow.chatoverflow.api.io.parameter.ListParameter
import org.codeoverflow.chatoverflow.registry.Impl

/**
  * A parameter holding a List<String> value.
  */
@Impl(impl = classOf[ListParameter])
class ListParameterImpl extends ListParameter {
  private var value: List[String] = null

  override def getType: Class[java.util.List[String]] = classOf[java.util.List[String]]

  override def serialize(): String = value.mkString(",")

  override def get(): java.util.List[String] = value.asJava

  override def deserialize(value: String): Unit = set(value.split(",").toSeq.asJava)

  override def set(value: java.util.List[String]): Unit = this.value = value.asScala.toList
}
