package org.codeoverflow.chatoverflow.registry

import org.codeoverflow.chatoverflow.api.plugin.configuration.{Requirement, Requirements}

import scala.collection.mutable

/**
  * The type registry holds all source / parameter types, that plugins are supposed to work with.
  * These are statically set at compile time.
  */
object TypeRegistry {

  private val registeredTypes = new mutable.HashMap[String, FrameworkType]()

  /**
    * Adds the list of all input types. Only supposed to be used in the IO-file.
    */
  def InputTypes(typeDefs: (String, FrameworkType)*): Unit = registerTypes(typeDefs)

  private def registerTypes(typeDefs: Seq[(String, FrameworkType)]): Unit = {
    typeDefs.foreach(typeDef => registeredTypes += typeDef)
  }

  /**
    * Adds the list of all output types. Only supposed to be used in the IO-file.
    */
  def OutputTypes(typeDefs: (String, FrameworkType)*): Unit = registerTypes(typeDefs)

  /**
    * Adds the list of all paramater types. Only supposed to be used in the IO-file.
    */
  def ParameterTypes(typeDefs: (String, FrameworkType)*): Unit = registerTypes(typeDefs)

  /**
    * Fancy scala logic to create a simple type from a triple of objects in the IO-file.
    *
    * @param triple the input triple of information
    * @return a filled framework type object with the information of the triple
    */
  implicit def tripleToFrameworkType(triple: (String, (Requirements, String, String) => Requirement[_], Requirement[_] => String)): FrameworkType = {
    FrameworkType(triple._1, triple._2, triple._3)
  }

  /**
    * Creates a typed requirement from serialized data. This is done resolving the dynamic typeString and
    * looking into the predefined types of the IO file, previously registered in the TypeRegistry.
    *
    * @param requirements        the requirements object, where the deserialized requirement should be added to
    * @param typeString          the dynamic type. must be registered in the IO-file first.
    * @param uniqueRequirementId the plugin unique requirement id from the configs
    * @param serializedValue     the serialized content, that is used to instantiate the requirements functionality
    * @return
    */
  def createRequirement(requirements: Requirements, typeString: String, uniqueRequirementId: String,
                        serializedValue: String): Requirement[_] = {
    registeredTypes(typeString).createRequirement(requirements, uniqueRequirementId, serializedValue)
  }

  /**
    * This method is used to load the serialized value out of an already instantiated requirement object.
    *
    * @param requirement the requirement with deserialized data in it
    * @return the serialized content, ready to be saved
    */
  def serializeRequirementContent(requirement: Requirement[_]): String = {
    registeredTypes(requirement.getTargetType.getName).serialize(requirement)
  }
}

/**
  * A framework type holds all information needed to create or serialize typed requirements.
  *
  * @param specificType      this is the specific framework type of the generic type, saved in configs and the registry
  * @param createRequirement this method takes a requirements container, an source id and the serialized content of the
  *                          requirement and uses it to create a typed requirement of the needed type
  * @param serialize         the serialize method takes the requirement and reads the content to serialize from it
  */
case class FrameworkType(specificType: String, createRequirement: (Requirements, String, String) => Requirement[_],
                         serialize: Requirement[_] => String)