import java.io.{File, PrintWriter}

/**
  * Represents a requirements class which is used by plugin devs to require inputs / outputs and parameters.
  *
  * @param requirementsDirectory the requirements directory in the api project
  * @param requirementType       the type of the requirements class (input / output / parameter)
  * @param files                 all found annotated source files which can be required
  */
class RequirementsFile(requirementsDirectory: File, requirementType: String, files: Seq[AnnotatedRequirement]) {

  /**
    * Creates the requirement file and saves it into the api.
    */
  def createFile(): Unit = {
    val fileContent = generateContent()

    val writer = new PrintWriter(new File(requirementsDirectory, s"$requirementType.java"))
    writer.print(fileContent)
    writer.close()
  }

  private def generateContent(): String =
    s"""package org.codeoverflow.chatoverflow.api.plugin.configuration;
       |
       |// THIS FILE IS GENERATED WHILE COMPILING. DO NOT CHANGE ANYTHING HERE!
       |
       |$generateImportStatements
       |
       |// THIS FILE IS GENERATED WHILE COMPILING. DO NOT CHANGE ANYTHING HERE!
       |
       |/**
       | * Select a $requirementType Requirement to get access to the desired platform or values.
       | */
       |public class $requirementType {
       |
       |    private final Requirements requirements;
       |
       |    $requirementType(Requirements requirements) {
       |        this.requirements = requirements;
       |    }
       |
       |$generateRequireMethods
       |}
       |""".stripMargin

  private def generateImportStatements: String = {
    val startingPoint = "main.java."
    val filesToImport = files.map(annotateReq => annotateReq.file.getAbsolutePath.replace(File.separatorChar, '.'))
    val classesToImport = filesToImport.map(filePath => filePath.substring(filePath.lastIndexOf(startingPoint) + startingPoint.length))

    classesToImport.map(_.replace(".java", "")).map(cls => s"import $cls;").mkString("\n|")
  }

  private def generateRequireMethods: String = {
    files.map(file => generateRequireMethod(file, shortVersion = false)
      ++ generateRequireMethod(file, shortVersion = true)).mkString
  }

  private def generateRequireMethod(requirement: AnnotatedRequirement, shortVersion: Boolean): String = {

    val className = requirement.file.getAbsolutePath.substring(
      requirement.file.getAbsolutePath.lastIndexOf(File.separator) + 1).replace(".java", "")

    val requiresValue = if (requirement.requires != "") requirement.requires else className

    val generatedName = className.replace(requirementType, "")

    val methodNameValue = if (requirement.methodName != "") {
      requirement.methodName.replace(" ", "")
    } else {
      generatedName.head.toLower + generatedName.tail
    }

    if (!shortVersion) {
      s"""    /**
         |     * Requires a $requiresValue which has to be specified by the user.
         |     *
         |     * @param uniqueRequirementId a plugin unique identifier which is stored for your plugin
         |     * @param displayName         a string to display to the user while setting your requirement
         |     * @param isOptional          true if this requirement is optional, false if mandatory
         |     * @return the requirement object. Use the get() method only at runtime!
         |     */
         |    public Requirement<$className> $methodNameValue(String uniqueRequirementId, String displayName, boolean isOptional) {
         |        return requirements.require$requirementType(uniqueRequirementId, displayName, isOptional, $className.class);
         |    }
         |
       |"""
    } else {
      val displayName = "\"" + s"${generatedName.map(c => if (c.isUpper) " " + c else c).mkString.trim}" + "\""

      s"""    /**
         |     * Requires a $requiresValue which has to be specified by the user.
         |     *
         |     * @param uniqueRequirementId a plugin unique identifier which is stored for your plugin
         |     * @return the requirement object. Use the get() method only at runtime!
         |     */
         |    public Requirement<$className> $methodNameValue(String uniqueRequirementId) {
         |        return requirements.require$requirementType(uniqueRequirementId, $displayName, false, $className.class);
         |    }
         |
       |"""
    }
  }
}

object RequirementsFile {
  def apply(requirementsDirectory: File, fileName: String, files: Seq[AnnotatedRequirement]): RequirementsFile =
    new RequirementsFile(requirementsDirectory, fileName, files)
}