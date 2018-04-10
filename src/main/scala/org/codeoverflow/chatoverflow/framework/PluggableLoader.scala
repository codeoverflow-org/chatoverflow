package org.codeoverflow.chatoverflow.framework

import java.io.File

import org.codeoverflow.chatoverflow.api.plugin.Pluggable

object PluggableLoader {

  def loadPluggables(jars: Array[File], cl: ClassLoader): Array[Class[Pluggable]] = ???

  private def extractPluggablesFromJarFiles(jars: Array[File], cl: ClassLoader): Array[Class[Pluggable]] = ???

  private def isPluggableClass(clazz: Class[_]): Boolean = ???

  private def createPluggableObjects(pluggables: Seq[Class[Pluggable]]): List[Pluggable] = ???

}
