package org.codeoverflow.chatoverflow.registry

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector
import org.reflections.Reflections
import org.reflections.scanners.{SubTypesScanner, TypeAnnotationsScanner}
import org.reflections.util.{ClasspathHelper, ConfigurationBuilder}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * The requirement type registry keeps track of all input/output/parameter types defined in the framework.
  *
  * @param requirementPackage The fully qualified name of the package, where all requirement types are defined
  */
class TypeRegistry(requirementPackage: String) extends WithLogger {
  private val requirementTypes = mutable.Map[String, Class[_]]()
  private val connectorTypes = mutable.Map[String, Class[_ <: Connector]]()
  private val requirementConnectorLinks = mutable.Map[String, String]()

  /**
    * Retrieves all registered requirement types (input / output / parameter).
    *
    * @return a seq of fully qualified type names
    */
  def getRequirementTypes: Map[String, Class[_]] = requirementTypes.toMap[String, Class[_]]

  /**
    * Returns the the complementary connector for a given api (interface) requirement type string.
    *
    * @param apiRequirementType the fully qualified type string from the api
    * @return some connector type string or none
    */
  def getRequirementConnectorLink(apiRequirementType: String): Option[String] =
    requirementConnectorLinks.get(apiRequirementType)

  /**
    * Retrieves all registered connector types.
    *
    * @return a seq of fully qualified type names
    */
  def getConnectorTypes: Seq[String] = connectorTypes.keys.toSeq

  /**
    * Clears the type registry, then scans the classpath for classes with the Impl-Annotation.
    * Requirements are added to the requirement-map, found connectors the connector-map
    */
  def updateTypeRegistry(): Unit = {

    // Start by clearing all known requirements and connectors
    requirementTypes.clear()
    connectorTypes.clear()

    // Use reflection magic to get all impl-annotated classes
    // FIXME: Does also find definitions not in the exact package - no problem right now
    val reflections: Reflections = new Reflections(new ConfigurationBuilder()
      .setUrls(ClasspathHelper.forPackage(requirementPackage))
      .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner()))
    val classes = reflections.getTypesAnnotatedWith(classOf[Impl])

    // Add classes to the list
    classes.forEach(clazz => {
      // Get annotated interface type
      val annotations = clazz.getAnnotationsByType[Impl](classOf[Impl])

      if (annotations.length != 1) {
        // This should never happen
        logger warn s"Should-have-Annotation-Type ${clazz.getName} has no annotation of type 'Impl'. What?"
      } else {

        // Add the mapping entry from interface type to implementation class
        requirementTypes += annotations(0).impl().getName -> clazz

        // Add the connector from the impl annotation
        // Note: Object is default value for parameter requirements which need no connector
        if (annotations(0).connector() != classOf[Connector]) {
          connectorTypes += annotations(0).connector().getName -> annotations(0).connector()

          // Link requirement type and connector type
          requirementConnectorLinks += annotations(0).impl().getName -> annotations(0).connector().getName
        }
      }
    })

    logger info s"Updated Type Registry. Added ${requirementTypes.size} requirement types."
    logger info s"Updated Type Registry. Added ${connectorTypes.size} connector types."
  }

  /**
    * Returns an optional implementation of a specific fully qualified interface type
    *
    * @param APIInterfaceTypeQualifiedName a fully qualified interface type string,
    *                                      e.g. "org.codeoverflow.[...].chat.TwitchChatInput"
    * @return the class, implementing this interface or none,
    *         if there is no such (previously registered) class
    */
  def getRequirementImplementation(APIInterfaceTypeQualifiedName: String): Option[Class[_]] = {
    requirementTypes.get(APIInterfaceTypeQualifiedName)
  }

  def getConnectorType(qualifiedConnectorName: String): Option[Class[_ <: Connector]] = {
    connectorTypes.get(qualifiedConnectorName)
  }

  /**
    * Returns a list of all classes that have an implementation of the specified interface in their hierarchy.
    * e.g. TwitchChatInput would be found, if use search for classes implementing Input, because
    * Input -> ChatInput -> TwitchChatInput.
    *
    * @param interfaceQualifiedName a fully qualified (java) interface name
    * @return a list of previously registered classes that match
    */
  def getAllSubTypeInterfaces(interfaceQualifiedName: String): List[Class[_]] = {
    val possibleClasses = ListBuffer[Class[_]]()

    for (clazz <- requirementTypes.values) {

      // Get all higher interfaces recursively
      val allInterfaces = ListBuffer[Class[_]]()
      addAllInterfacesToListBuffer(clazz, allInterfaces)

      // Match fully qualified interface type names
      if (allInterfaces.exists(interface => interface.getName.equals(interfaceQualifiedName))) {
        possibleClasses += clazz
      }
    }

    possibleClasses.toList
  }

  /**
    * This helper method helps to add all higher interfaces in a recursive manner
    */
  private def addAllInterfacesToListBuffer(clazz: Class[_], listBuffer: ListBuffer[Class[_]]): Unit = {
    listBuffer ++= clazz.getInterfaces
    clazz.getInterfaces.foreach(newClazz => addAllInterfacesToListBuffer(newClazz, listBuffer))
  }
}
