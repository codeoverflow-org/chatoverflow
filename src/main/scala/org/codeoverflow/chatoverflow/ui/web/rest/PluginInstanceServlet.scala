package org.codeoverflow.chatoverflow.ui.web.rest

import org.codeoverflow.chatoverflow.api.io
import org.codeoverflow.chatoverflow.api.plugin.configuration
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.{PluginInstance, Requirement}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class PluginInstanceServlet extends JsonServlet {

  get("/") {
    chatOverflow.pluginInstanceRegistry.getAllPluginInstances.map(pluginInstanceToDTO)

  }

  get("/:instanceName") {
    val instanceName = params("instanceName")

    chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName).map(pluginInstanceToDTO)
  }

  get("/:instanceName/requirements") {
    val instanceName = params("instanceName")
    val pluginInstance = chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName)
    val requirements = pluginInstance.get.getRequirements.getRequirementMap
    val returnSeq = ListBuffer[Requirement]()

    requirements.forEach((uniqueRequirementID, requirement) => returnSeq += Requirement(uniqueRequirementID,
      requirement.getName, requirement.isOptional, requirement.isSet,
      requirement.asInstanceOf[configuration.Requirement[io.Serializable]].get().serialize(),
      requirement.getTargetType.getName))

    returnSeq
  }

  private def pluginInstanceToDTO(pluginInstance: org.codeoverflow.chatoverflow.instance.PluginInstance) = {
    PluginInstance(pluginInstance.instanceName, pluginInstance.getPluginTypeName,
      pluginInstance.getPluginTypeAuthor, pluginInstance.isRunning,
      pluginInstance.getRequirements.getRequirementMap.keySet().asScala.toList)

  }


}
