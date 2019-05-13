package org.codeoverflow.chatoverflow.ui.web.rest

import org.codeoverflow.chatoverflow.api.io
import org.codeoverflow.chatoverflow.api.plugin.configuration
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.{PluginInstance, Requirement}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class PluginInstanceController extends JsonServlet {

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

  get("/:instanceName/log") {
    val instanceName = params("instanceName")
    val startIndex = if (params.isDefinedAt("startIndex")) Some(params("startIndex")) else None
    val pluginInstance = chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName)
    val logMessages = pluginInstance.get.getPluginManager.getLogMessages

    val index = startIndex.getOrElse("0")
    val msg = logMessages.toArray.drop(Integer.parseInt(index))
    msg.toSeq

  }

  private def pluginInstanceToDTO(pluginInstance: org.codeoverflow.chatoverflow.instance.PluginInstance) = {
    PluginInstance(pluginInstance.instanceName, pluginInstance.getPluginTypeName,
      pluginInstance.getPluginTypeAuthor, pluginInstance.isRunning,
      pluginInstance.getRequirements.getRequirementMap.keySet().asScala.toList)

  }


}
