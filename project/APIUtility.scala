import java.io.File

import sbt.internal.util.ManagedLogger
import sbt.io.IO

import scala.io.Source

/**
  * The API utility encapsulates methods for building the chatoverflow API.
  *
  * @param logger The current logger instance. Usually: {{{streams.value.log}}}
  */
class APIUtility(logger: ManagedLogger) {

  /**
    * Generates the requirement classes input, output and parameter from annotated types in the api.
    *
    * @param sourceDirectory the sourceDirectory from sbt {{{sourceDirectory.value}}}
    */
  def generatedRequirements(sourceDirectory: File): Unit = {
    // Short explanation: Using reflection would cause strong circular dependencies between api and build environment.
    // So, looking up the source files is considered a dirty but "cleaner" solution

    val apiFolder = "main/java/org/codeoverflow/chatoverflow/api"
    val ioFolder = s"$apiFolder/io"
    val configurationFolder = s"$apiFolder/plugin/configuration"
    val requirementTypes = List("input", "output", "parameter")
    val requiresFieldName = "requires"
    val methodFieldName = "methodName"

    // Note: We assume clean java files. Interface name == fileName and only one annotated interface per file
    val requirementRegex =
      """@IsRequirement\s*(\((([a-zA-Z]*)\s*=\s*"([^"]*)",?\s*)?(([a-zA-Z]*)\s*=\s*"([^"]*)",?\s*)?\))?""".r

    val ioDirectory = new File(sourceDirectory, ioFolder)
    val sourceFiles = requirementTypes.map(req => new File(ioDirectory, req)).map(getAllChilds)

    // Creates a list with 3 sub lists of all files of a kind (input/output/parameter) containing requirement annotations
    val filesWithRequirements =
      for (sourceFilesOfAKind <- sourceFiles) yield {
        for (sourceFile <- sourceFilesOfAKind) yield {
          val content = Source.fromFile(sourceFile).mkString

          // This is (especially the group ids) highly dependent of the regex string and the IsRequirement-Annotation
          requirementRegex.findFirstMatchIn(content) match {
            case None => None
            case Some(regexMatch) =>
              var requires = ""
              var methodName = ""

              if (regexMatch.subgroups.indexOf(requiresFieldName) != -1)
                requires = regexMatch.group(regexMatch.subgroups.indexOf(requiresFieldName) + 2)

              if (regexMatch.subgroups.indexOf(methodFieldName) != -1)
                methodName = regexMatch.group(regexMatch.subgroups.indexOf(methodFieldName) + 2)

              Some(AnnotatedRequirement(sourceFile, requires, methodName))
          }
        }
      }.filter(_.isDefined).map(_.get)

    RequirementsFile(new File(sourceDirectory, configurationFolder), "Input", filesWithRequirements.head).createFile()
    RequirementsFile(new File(sourceDirectory, configurationFolder), "Output", filesWithRequirements(1)).createFile()
    RequirementsFile(new File(sourceDirectory, configurationFolder), "Parameter", filesWithRequirements(2)).createFile()
  }

  /**
    * Generates the API version file based on the values in the build file.
    *
    * @param sourceDirectory the sourceDirectory from sbt {{{sourceDirectory.value}}}
    * @param majorVersion    the major api version
    * @param minorVersion    the minor api version
    */
  def generateAPIVersionFile(sourceDirectory: File, majorVersion: Int, minorVersion: Int): Unit = {
    val file = new File(sourceDirectory, "main/java/org/codeoverflow/chatoverflow/api/APIVersion.java")
    IO.write(file,
      """package org.codeoverflow.chatoverflow.api;
        |
        |/**
        | * THIS CLASS IS GENERATED WHILE COMPILING. DO CHANGE THE VERSION NUMBERS IN THE APIS BUILD.SBT!
        | */
        |public class APIVersion {
        |    public static final int MAJOR_VERSION = %d;
        |    public static final int MINOR_VERSION = %d;
        |}
        |""".stripMargin.format(majorVersion, minorVersion))
  }

  private def getAllChilds(directory: File): Seq[File] = {
    directory.listFiles().filter(_.isFile) ++ directory.listFiles().filter(_.isDirectory).flatMap(getAllChilds)
  }

}

case class AnnotatedRequirement(file: File, requires: String = "", methodName: String = "")

object APIUtility {
  def apply(logger: ManagedLogger): APIUtility = new APIUtility(logger)
}