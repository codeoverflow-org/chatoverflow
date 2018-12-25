package org.codeoverflow.chatoverflow2.requirement

import org.reflections.Reflections
import org.reflections.scanners.{SubTypesScanner, TypeAnnotationsScanner}
import org.reflections.util.{ClasspathHelper, ConfigurationBuilder}

class RequirementTypeRegistry(servicePackage: String) {

  def updateTypeRegistry(): Unit = {

    val reflections: Reflections = new Reflections(new ConfigurationBuilder()
      .setUrls(ClasspathHelper.forPackage(servicePackage))
      .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner()))

    val requirementTypes = reflections.getTypesAnnotatedWith(classOf[Impl])
    println(requirementTypes.size)
  }

  // TODO: Save Impl type, interface type, interface super type
}
