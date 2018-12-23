package org.codeoverflow.chatoverflow2.framework

object PluginVersionState extends Enumeration {
  type PluginVersionState = Value
  val Untested, FullyCompatible, MajorCompatible, NotCompatible = Value
}