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
    */
  def generatedRequirements(sourceDirectory: File): Unit = {
    println("I'm not implemented yet.")

    // Short explanation: Using reflection would cause circular dependencies between api and build environment.
    // So, looking up the source files is considered a dirty but "cleaner" solution

    val ioDirectory = new File(sourceDirectory, "main/java/org/codeoverflow/chatoverflow/api/io")
    val subDiretories = Seq(new File(ioDirectory, "input"),
      new File(ioDirectory, "output"), new File(ioDirectory, "parameter"))

    // Uses recursion to retrieve all (nested) source files
    val sourceFiles = subDiretories.flatMap(getAllChilds)

    // Note: We assume clean java files. Interface name == fileName and only one annotated interface per file
    val requirementRegex =
      """@IsRequirement\s*(\((([a-zA-Z]*)\s*=\s*"([^"]*)",?\s*)?(([a-zA-Z]*)\s*=\s*"([^"]*)",?\s*)?\))?""".r

    val filesWithRequirements =
      for (sourceFile <- sourceFiles) yield {
        val content = Source.fromFile(sourceFile).mkString

        // This is (especially the group ids) highly dependent of the regex string and the IsRequirement-Annotation
        requirementRegex.findFirstMatchIn(content) match {
          case None =>
          case Some(regexMatch) =>
            var requires = ""
            var methodName = ""

            if (regexMatch.group(3) == "requires")
              requires = regexMatch.group(4)

            if (regexMatch.group(3) == "methodName")
              methodName = regexMatch.group(4)

            if (regexMatch.group(6) == "requires")
              requires = regexMatch.group(7)

            if (regexMatch.group(6) == "methodName")
              methodName = regexMatch.group(7)

            AnnotatedRequirement(sourceFile, requires, methodName)
        }
      }

    // TODO: Refactor in a way, that the structure (input/output/parameter) is kept
    // TODO: Now, create a ConfigurationFile (Input/Output/Parameter) which is then saved
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

  case class AnnotatedRequirement(file: File, requires: String = "", methodName: String = "")

}

object APIUtility {
  def apply(logger: ManagedLogger): APIUtility = new APIUtility(logger)
}