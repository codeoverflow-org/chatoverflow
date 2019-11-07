package org.codeoverflow.chatoverflow.requirement.parameter

import java.util.Collections
import java.util.List

import collection.JavaConverters._
import org.codeoverflow.chatoverflow.api.io.parameter.ListParameter
import org.codeoverflow.chatoverflow.registry.Impl

import scala.collection.mutable.ListBuffer


@Impl(impl = classOf[ListParameter])
class ListParameterImpl extends ListParameter {
  private var value: List[String] = null

  override def getType: Class[List[String]] = classOf[List[String]]

  override def serialize(): String = {
    var out = ""
    for (part <- value.toArray()) {
      if (out == "") {
        out = out + part
      } else {
        out = out + "," + part
      }
    }
    out
  }

  override def get(): List[String] = value

  override def deserialize(value: String): Unit = {
    var splits = value.split(",");
    if (splits.length == 1) {
      set(Collections.singletonList[String](splits(0)))
    } else {
      var list: ListBuffer[String] = ListBuffer[String]()
      for (part <- splits) {
        list += part
      }
      set(mutableSeqAsJavaList[String](list))
    }
  }

  override def set(value: List[String]): Unit = this.value = value

}
