package org.codeoverflow.chatoverflow.registry

import org.codeoverflow.chatoverflow.api.plugin.configuration.{Requirement, Requirements}

import scala.collection.mutable

object TypeRegistry {

  private val registeredTypes = new mutable.HashMap[String, FrameworkType]()

  def InputTypes(typeDefs: (String, FrameworkType)*): Unit = registerTypes(typeDefs)

  def OutputTypes(typeDefs: (String, FrameworkType)*): Unit = registerTypes(typeDefs)

  def registerTypes(typeDefs: Seq[(String, FrameworkType)]): Unit = {
    typeDefs.foreach(typeDef => registeredTypes += typeDef)
  }

  def ParameterTypes(typeDefs: (String, FrameworkType)*): Unit = registerTypes(typeDefs)

  implicit def tripleToFrameworkType(quad: (String, (Requirements, String, String, Boolean, String) => Requirement[_], Requirement[_] => String)): FrameworkType = {
    FrameworkType(quad._1, quad._2, quad._3)
  }

  def createRequirement(requirements: Requirements, typeString: String, uniqueRequirementId: String,
                        name: String, isOptional: Boolean, serializedValue: String): Requirement[_] = {
    registeredTypes(typeString).createRequirement(requirements, uniqueRequirementId, name, isOptional, serializedValue)
  }

  def serializeRequirementContent(requirement: Requirement[_]): String = {
    registeredTypes(requirement.getTargetType.getName).serialize(requirement)
  }
}

case class FrameworkType(specificType: String, createRequirement: (Requirements, String, String, Boolean, String) => Requirement[_],
                         serialize: Requirement[_] => String)