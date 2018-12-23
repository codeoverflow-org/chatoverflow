package org.codeoverflow.chatoverflow2.framework

import org.codeoverflow.chatoverflow.api.APIVersion
import org.codeoverflow.chatoverflow.api.plugin.Pluggable
import org.codeoverflow.chatoverflow2.WithLogger
import org.codeoverflow.chatoverflow2.framework.PluginVersionState.PluginVersionState

class Plugin(pluggable: Pluggable) extends WithLogger {
  private var pluginVersionState = PluginVersionState.Untested

  def getAuthor: String = pluggable.getAuthor

  def getDescription: String = pluggable.getDescription

  def getState: PluginVersionState = pluginVersionState

  def testState: PluginVersionState = {
    if (getMajorAPIVersion != APIVersion.MAJOR_VERSION) {
      logger info s"Plugin '$getName' has different major API version: $getMajorAPIVersion."
      pluginVersionState = PluginVersionState.NotCompatible

    } else if (getMinorAPIVersion != APIVersion.MINOR_VERSION) {
      logger info s"Plugin '$getName' has different minor API version: $getMinorAPIVersion."
      pluginVersionState = PluginVersionState.MajorCompatible

    } else {
      logger info s"Plugin '$getName' has no difference in API version numbers."
      pluginVersionState = PluginVersionState.FullyCompatible
    }

    pluginVersionState
  }

  def getName: String = pluggable.getName

  def getMajorAPIVersion: Int = pluggable.getMajorAPIVersion

  def getMinorAPIVersion: Int = pluggable.getMinorAPIVersion
}
