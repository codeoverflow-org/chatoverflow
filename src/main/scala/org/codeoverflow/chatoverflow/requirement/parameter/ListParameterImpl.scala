package org.codeoverflow.chatoverflow.requirement.parameter

import java.util.List

import collection.JavaConverters._
import org.codeoverflow.chatoverflow.api.io.parameter.ListParameter
import org.codeoverflow.chatoverflow.registry.Impl


/**
  * A parameter holding a List<String> value.
  */
@Impl(impl = classOf[ListParameter])
class ListParameterImpl extends ListParameter {
  private var value: List[String] = null

  override def getType: Class[List[String]] = classOf[List[String]]

  override def serialize(): String = {
    value.asScala.mkString(",")
  }

  override def get(): List[String] = value

  override def deserialize(value: String): Unit = {
    set(value.split(",").toSeq.asJava)
  }

  override def set(value: List[String]): Unit = this.value = value

}
