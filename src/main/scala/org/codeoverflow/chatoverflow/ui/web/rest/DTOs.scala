package org.codeoverflow.chatoverflow.ui.web.rest

object DTOs {

  case class PluginType(name: String, author: String, description: String,
                        majorAPIVersion: Int, minorAPIVersion: Int, state: String)

  case class PluginInstance(instanceName: String, pluginName: String, pluginAuthor: String, isRunning: Boolean, requirementIDs: Seq[String])

  case class ConfigInfo(name: String, apiMajorVersion: Int, apiMinorVersion: Int, pluginFolderPath: String,
                        configFolderPath: String, requirementPackage: String, pluginDataPath: String)

  case class Requirement(uniqueRequirementId: String, name: String, isOptional: Boolean, isSet: Boolean, value: String, targetType: String)

  case class Types(pluginTypes: Seq[PluginType], requirementTypes: RequirementTypes, connectorTypes: Seq[String])

  case class RequirementTypes(input: Seq[String], output: Seq[String], parameter: Seq[String])

  case class APIAndSpecificType(interface: String, implementation: String, connector: String, found: Boolean)

  case class SubTypes(interface: String, subtypes: Seq[String])

  case class ConnectorDetails(found: Boolean, sourceIdentifier: String, uniqueTypeString: String,
                              areCredentialsSet: Boolean, isRunning: Boolean, requiredCredentialKeys: Seq[String],
                              optionalCredentialKeys: Seq[String])

  case class CredentialsDetails(found: Boolean, requiredCredentials: Map[String, String] = Map[String, String](),
                                optionalCredentials: Map[String, String] = Map[String, String]())

  case class PluginInstanceRef(instanceName: String, pluginName: String, pluginAuthor: String)

  case class ResultMessage(success: Boolean, message: String = "")

  case class ConnectorRef(sourceIdentifier: String, uniqueTypeString: String)

  case class Password(password: String)

}
