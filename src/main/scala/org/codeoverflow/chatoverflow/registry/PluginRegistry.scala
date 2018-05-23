package org.codeoverflow.chatoverflow.registry

import org.codeoverflow.chatoverflow.api.plugin.Plugin

import scala.collection.mutable

object PluginRegistry {

  private val plugins = mutable.Map[String, Plugin]()

  // val configs pluginId -> Configuration -> ... -> type & sourceId möchte ich am schluss haben
  // Configuration enthält Bedürfnisse (Liste an In- und Outputs, z.B. ChatInput) des Plugins + sourceIds

}