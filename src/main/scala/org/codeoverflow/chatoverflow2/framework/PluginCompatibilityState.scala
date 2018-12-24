package org.codeoverflow.chatoverflow2.framework

/**
  * The plugin compatibility state does describe if the API version (Major.Minor) of a plugin is
  * compatible with the framework API Version.
  */
object PluginCompatibilityState extends Enumeration {
  type PluginCompatibilityState = Value
  val Untested, FullyCompatible, MajorCompatible, NotCompatible = Value
}