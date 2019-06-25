package org.codeoverflow.chatoverflow.ui.web.rest.plugin

import org.codeoverflow.chatoverflow.api.io
import org.codeoverflow.chatoverflow.api.plugin.configuration
import org.codeoverflow.chatoverflow.configuration.ConfigurationService
import org.codeoverflow.chatoverflow.ui.web.JsonServlet
import org.codeoverflow.chatoverflow.ui.web.rest.DTOs._
import org.scalatra.swagger.Swagger

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class PluginInstanceController(implicit val swagger: Swagger) extends JsonServlet with PluginInstanceControllerDefinition {

  get("/", operation(getInstances)) {
    authKeyRequired {
      chatOverflow.pluginInstanceRegistry.getAllPluginInstances.map(pluginInstanceToDTO)
    }
  }

  get("/:instanceName", operation(getInstance)) {
    authKeyRequired {
      val instanceName = params("instanceName")

      chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName).map(pluginInstanceToDTO)
    }
  }

  post("/start", operation(startInstance)) {
    authKeyRequired {
      parsedAs[PluginInstanceName] {
        case PluginInstanceName(instanceName) =>

          if (!chatOverflow.isLoaded) {
            ResultMessage(success = false, "Framework not loaded.")

          } else {
            val pluginInstance = chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName)

            if (pluginInstance.isEmpty) {
              ResultMessage(success = false, "Plugin instance not found.")

            } else if (pluginInstance.get.isRunning) {
              ResultMessage(success = false, "Plugin instance already running.")

            } else if (!pluginInstance.get.getRequirements.isComplete) {
              ResultMessage(success = false, "Not all required requirements have been set.")

            } else if (!pluginInstance.get.start()) {
              ResultMessage(success = false, "Unable to start plugin.")

            } else {
              ResultMessage(success = true)
            }
          }
      }
    }
  }

  post("/stop", operation(stopInstance)) {
    authKeyRequired {
      parsedAs[PluginInstanceName] {
        case PluginInstanceName(instanceName) =>

          if (!chatOverflow.isLoaded) {
            ResultMessage(success = false, "Framework not loaded.")

          } else {
            val pluginInstance = chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName)

            if (pluginInstance.isEmpty) {
              ResultMessage(success = false, "Plugin instance not found.")

            } else if (!pluginInstance.get.isRunning) {
              ResultMessage(success = false, "Plugin instance is not running.")

            } else {
              pluginInstance.get.stopPlease()
              ResultMessage(success = true)
            }
          }
      }
    }
  }

  get("/:instanceName/requirements", operation(getRequirements)) {
    authKeyRequired {
      val instanceName = params("instanceName")
      val returnSeq = ListBuffer[Requirement]()
      val pluginInstance = chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName)

      if (pluginInstance.isEmpty) {
        returnSeq

      } else {
        val requirements = pluginInstance.get.getRequirements.getRequirementMap

        requirements.forEach((uniqueRequirementID, requirement) =>
          returnSeq += createRequirement(uniqueRequirementID, requirement))

        returnSeq.toList
      }
    }
  }

  get("/:instanceName/requirements/:requirementID", operation(getRequirement)) {
    authKeyRequired {
      val instanceName = params("instanceName")
      val requirementID = params("requirementID")

      val pluginInstance = chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName)

      if (pluginInstance.isEmpty) {
        Requirement("", "", isOptional = false, isSet = false, "", "")
      } else {
        val requirement = pluginInstance.get.getRequirements.getRequirementById(requirementID)

        if (!requirement.isPresent) {
          Requirement("", "", isOptional = false, isSet = false, "", "")
        } else {
          createRequirement(requirementID, requirement.get)
        }
      }
    }
  }

  put("/:instanceName/requirements/:requirementID", operation(putRequirement)) {
    authKeyRequired {
      parsedAs[RequirementInfo] {
        case RequirementInfo(targetType, value) =>
          val instanceName = params("instanceName")
          val requirementID = params("requirementID")

          if (!chatOverflow.isLoaded) {
            ResultMessage(success = false, "Framework not loaded.")

          } else {
            val pluginInstance = chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName)

            if (pluginInstance.isEmpty) {
              ResultMessage(success = false, "Plugin instance not found.")

            } else if (pluginInstance.get.isRunning) {
              ResultMessage(success = false, "Plugin is running.")

            } else if (!pluginInstance.get.getRequirements.getRequirementById(requirementID).isPresent) {
              ResultMessage(success = false, "Requirement not found.")
            } else {

              if (!ConfigurationService.fulfillRequirementByDeserializing(instanceName, requirementID, targetType,
                value, chatOverflow.pluginInstanceRegistry, chatOverflow.typeRegistry)) {

                ResultMessage(success = false, "Unable to set the requirement.")
              } else {
                chatOverflow.save()
                ResultMessage(success = true)
              }
            }
          }
      }
    }
  }

  delete("/:instanceName/requirements/:requirementID", operation(deleteRequirement)) {
    authKeyRequired {
      val instanceName = params("instanceName")
      val requirementID = params("requirementID")

      if (!chatOverflow.isLoaded) {
        ResultMessage(success = false, "Framework not loaded.")

      } else {
        val pluginInstance = chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName)

        if (pluginInstance.isEmpty) {
          ResultMessage(success = false, "Plugin instance not found.")

        } else if (pluginInstance.get.isRunning) {
          ResultMessage(success = false, "Plugin is running.")

        } else if (!pluginInstance.get.getRequirements.getRequirementById(requirementID).isPresent) {
          ResultMessage(success = false, "Requirement not found.")
        } else {

          if (!pluginInstance.get.getRequirements.unsetRequirementById(requirementID)) {

            ResultMessage(success = false, "Unable to remove the requirement. Already removed.")
          } else {
            chatOverflow.save()
            ResultMessage(success = true)
          }
        }
      }

    }
  }

  get("/:instanceName/log", operation(getLog)) {
    authKeyRequired {
      val instanceName = params("instanceName")
      val startIndex = if (params.isDefinedAt("startIndex")) Some(params("startIndex")) else None
      val pluginInstance = chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName)
      val logMessages = pluginInstance.get.getPluginManager.getLogMessages
        .asScala.map(logMessage => PluginLogMessageDTO(logMessage.getMessage, logMessage.getTimestamp.toString))

      val index = startIndex.getOrElse("0")
      val msg = logMessages.toArray.drop(Integer.parseInt(index))
      msg.toSeq
    }
  }

  post("/", operation(postInstance)) {
    authKeyRequired {
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
  }

  delete("/:instanceName", operation(deleteInstance)) {
    authKeyRequired {
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
  }

  private def pluginInstanceToDTO(pluginInstance: org.codeoverflow.chatoverflow.instance.PluginInstance)

  = {
    PluginInstance(pluginInstance.instanceName, pluginInstance.getPluginTypeName,
      pluginInstance.getPluginTypeAuthor, pluginInstance.isRunning,
      pluginInstance.getRequirements.getRequirementMap.keySet().asScala.toList)
  }

  private def createRequirement(requirementID: String, requirement: configuration.Requirement[_ <: io.Serializable]): Requirement

  = {
    Requirement(requirementID,
      requirement.getName, requirement.isOptional, requirement.isSet,
      {
        // Can be null if the requirement was freshly created in this run
        val requirementContent = requirement.asInstanceOf[configuration.Requirement[io.Serializable]].get()

        if (requirementContent == null) {
          ""
        } else {
          requirementContent.serialize()
        }
      },
      requirement.getTargetType.getName)
  }
}
