import java.io.File

import sbt.internal.util.ManagedLogger
import sbt.io.IO

import scala.collection.mutable.ListBuffer

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

    // Short explanation: Using reflection would cause circular dependencies between classes and build environment.
    // So, looking up the source files is considered a dirty, but cleaner solution

    val ioDirectory = new File(sourceDirectory, "main/java/org/codeoverflow/chatoverflow/api/io")
    val subDiretories = Seq(new File(ioDirectory, "input"),
      new File(ioDirectory, "output"), new File(ioDirectory, "parameter"))

    var sourceFiles = ListBuffer[File]()

    sourceFiles ++= subDiretories.flatMap(file => file.listFiles().toList)

    // Recursively add child files
    for (i <- 1 to 5) {
      sourceFiles = addChilds(sourceFiles)
    }

    // TODO: Now, every file should be parsed for the Annotation and the interface name using regex
  }

  private def addChilds(sourceFiles: ListBuffer[File]): ListBuffer[File] = {
    for (sourceFile <- sourceFiles) {
      if (sourceFile.isDirectory) {
        sourceFiles ++= sourceFile.listFiles().filter(file => !sourceFiles.contains(file))
      }
    }
    sourceFiles
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

}

object APIUtility {
  def apply(logger: ManagedLogger): APIUtility = new APIUtility(logger)
}