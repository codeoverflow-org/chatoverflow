package org.codeoverflow.chatoverflow2.requirement

import org.codeoverflow.chatoverflow2.WithLogger
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
class RequirementTypeRegistry(requirementPackage: String) extends WithLogger {
  private var requirementTypes = mutable.Map[String, Class[_]]()

  /**
    * Clears the type registry, then scans the classpath for classes with the Impl-Annotation.
    */
  def updateTypeRegistry(): Unit = {

    // Start by clearing all known requirements
    requirementTypes.clear()

    // Use reflection magic to get all impl-annotated classes
    // FIXME: Does also find requirement definitions not in the exact package - no problem right now
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
        logger warn s"Requirement type ${clazz.getName} has no annotation of type 'Impl'."
      } else {

        // Add the mapping entry from interface type to implementation class
        requirementTypes += annotations(0).value().getName -> clazz
      }
    })

    logger info s"Updated Requirement Type Registry. Added ${requirementTypes.size} types."
  }

  /**
    * Returns an optional implementation of a specific fully qualified interface type
    *
    * @param APIInterfaceTypeQualifiedName a fully qualified interface type string,
    *                                      e.g. "org.codeoverflow.[...].chat.TwitchChatInput"
    * @return the class, implementing this interface or none,
    *         if there is no such (previously registered) class
    */
  def getImplementation(APIInterfaceTypeQualifiedName: String): Option[Class[_]] = {
    requirementTypes.get(APIInterfaceTypeQualifiedName)
  }

  /**
    * Returns a list of all classes that have an implementation of the specified interface in their hierarchy.
    * e.g. TwitchChatInput would be found, if use search for classes implementing Input, because
    * Input -> ChatInput -> TwitchChatInput.
    *
    * @param interfaceQualifiedName a fully qualified (java) interface name
    * @return a list of previously registered classes that match
    */
  def getAllClassesImplementingInterface(interfaceQualifiedName: String): List[Class[_]] = {
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
