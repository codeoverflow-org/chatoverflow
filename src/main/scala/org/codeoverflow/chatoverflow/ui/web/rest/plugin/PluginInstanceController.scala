package org.codeoverflow.chatoverflow.ui.web.rest.plugin

import org.codeoverflow.chatoverflow.api.io
import org.codeoverflow.chatoverflow.api.plugin.configuration
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs.{PluginInstance, PluginInstanceRef, Requirement, ResultMessage}
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

  post("/", operation(postInstance)) {
    parsedAs[PluginInstanceRef] {
      case PluginInstanceRef(instanceName, pluginName, pluginAuthor) =>
        if (!chatOverflow.isLoaded) {
          ResultMessage(success = false, "Framework not loaded.")

        } else {

          // Check existence of key first
          if (chatOverflow.pluginInstanceRegistry.pluginInstanceExists(instanceName)) {
            ResultMessage(success = false, "Plugin instance already exists.")

          } else if (!chatOverflow.pluginFramework.pluginExists(pluginName, pluginAuthor)) {
            ResultMessage(success = false, "Plugin type does not exist.")

          } else {
            val pluginType = chatOverflow.pluginFramework.getPlugin(pluginName, pluginAuthor)

            if (!chatOverflow.pluginInstanceRegistry.addPluginInstance(instanceName, pluginType.get)) {
              ResultMessage(success = false, "Unable to create new plugin instance.")
            } else {
              chatOverflow.save()
              ResultMessage(success = true)
            }
          }
        }
    }
  }

  delete("/:instanceName", operation(deleteInstance)) {
    val instanceName = params("instanceName")

    if (!chatOverflow.isLoaded) {
      ResultMessage(success = false, "Framework not loaded.")

    } else {

      val pluginInstance = chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName)

      if (pluginInstance.isEmpty) {
        ResultMessage(success = false, "Plugin instance not found.")

      } else if (pluginInstance.get.isRunning) {
        ResultMessage(success = false, "Plugin instance is running.")

      } else if (!chatOverflow.pluginInstanceRegistry.removePluginInstance(instanceName)) {
        ResultMessage(success = false, "Unable to remove plugin instance.")

      } else {
        chatOverflow.save()
        ResultMessage(success = true)
      }
    }
  }

  private def pluginInstanceToDTO(pluginInstance: org.codeoverflow.chatoverflow.instance.PluginInstance) = {
    PluginInstance(pluginInstance.instanceName, pluginInstance.getPluginTypeName,
      pluginInstance.getPluginTypeAuthor, pluginInstance.isRunning,
      pluginInstance.getRequirements.getRequirementMap.keySet().asScala.toList)
  }
}
