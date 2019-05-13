package org.codeoverflow.chatoverflow.ui.web.rest.plugin

import org.codeoverflow.chatoverflow.api.io
import org.codeoverflow.chatoverflow.api.plugin.configuration
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.{PluginInstance, Requirement}
import org.scalatra.swagger.Swagger

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class PluginInstanceController(implicit val swagger: Swagger) extends JsonServlet with PluginInstanceControllerDefinition {

  get("/", operation(getInstances)) {
    chatOverflow.pluginInstanceRegistry.getAllPluginInstances.map(pluginInstanceToDTO)

  }

  get("/:instanceName", operation(getInstance)) {
    val instanceName = params("instanceName")

    chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName).map(pluginInstanceToDTO)
  }

  get("/:instanceName/requirements", operation(getRequirements)) {
    val instanceName = params("instanceName")
    val pluginInstance = chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName)
    val requirements = pluginInstance.get.getRequirements.getRequirementMap
    val returnSeq = ListBuffer[Requirement]()

    requirements.forEach((uniqueRequirementID, requirement) => returnSeq += Requirement(uniqueRequirementID,
      requirement.getName, requirement.isOptional, requirement.isSet,
      requirement.asInstanceOf[configuration.Requirement[io.Serializable]].get().serialize(),
      requirement.getTargetType.getName))

    returnSeq.toList
  }

  get("/:instanceName/log", operation(getLog)) {
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
