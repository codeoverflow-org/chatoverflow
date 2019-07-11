import java.io.File
import sbt.internal.util.ManagedLogger
import sbt.io.IO

/**
  * The API utility encapsulates methods for building the chatoverflow API.
  * @param logger The current logger instance. Usually: {{{streams.value.log}}}
  */
class APIUtility(logger: ManagedLogger) {

  /**
    * Generates the requirement classes input, output and parameter from annotated types in the api.
    */
  def generatedRequirements(): Unit = {
    println("I'm not implemented yet.")

    // Not working
    /*
    val reflections: Reflections = new Reflections(new ConfigurationBuilder()
      .setUrls(ClasspathHelper.forPackage(...))
      .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner()))
    val classes = reflections.getTypesAnnotatedWith(classOf[IsRequirement])
    */
  }

  /**
    * Generates the API version file based on the values in the build file.
    * @param sourceDirectory the sourceDirectory from sbt {{{sourceDirectory.value}}}
    * @param majorVersion the major api version
    * @param minorVersion the minor api version
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