package org.codeoverflow.chatoverflow.requirement.parameter

import java.util.{Collections, List}

import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.api.io.parameter.MapParameter

import collection.JavaConverters._
import java.util.Map
/**
  * A parameter holding a Map<String, String> value.
  */
@Impl(impl = classOf[MapParameter])
class MapParameterImpl extends MapParameter{
  private var value: Map[String, String] = null

  override def getType: Class[Map[String, String]] = classOf[Map[String, String]]

  override def serialize(): String = {
    var out = ""
    value.keySet().forEach(key => {
      var valueSet = value.get(key)
      if(out == ""){
        out = out + "("+key+";"+valueSet+")"
      }else{
        out = out + ",("+key+";"+valueSet+")"
      }
    })
    out
  }

  override def get(): Map[String, String] = value

  override def deserialize(value: String): Unit = {
    var splits = value.split(",");
    if (splits.length == 1) {
      var obj = splits(0)
      obj = obj.replaceAll("\\(" ,"").replaceAll("\\)","").trim
      splits = obj.split(";")
      if(splits.length == 2){
        set(Collections.singletonMap[String, String](splits(0), splits(1)))
      }
    } else {
      var map = scala.collection.mutable.Map[String, String]()
      for (part <- splits) {
        var obj: String = part.replaceAll("\\(" ,"").replaceAll("\\)","").trim
        var parts = obj.split(";")
        if(parts.length == 2){
          map += (parts(0) -> parts(1))
        }
      }
      if(map.nonEmpty) set(mutableMapAsJavaMap[String, String](map))
    }
  }

  override def set(value: Map[String, String]): Unit = this.value = value

}
