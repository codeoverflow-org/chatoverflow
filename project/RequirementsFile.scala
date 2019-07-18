import java.io.{File, PrintWriter}

class RequirementsFile(requirementsDirectory: File, requirementType: String, files: Seq[AnnotatedRequirement]) {

  def createFile(): Unit = {
    val fileContent = generateContent()

    val writer = new PrintWriter(new File(requirementsDirectory, s"${requirementType}Gen.java"))
    writer.print(fileContent)
    writer.close()
  }

  private def generateContent(): String =
    s"""package org.codeoverflow.chatoverflow.api.plugin.configuration;
       |
       |$generateImportStatements
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
       |
       |}
       |""".stripMargin

  private def generateImportStatements: String =
    """import org.codeoverflow.chatoverflow.api.io.input.FileInput;
      |import org.codeoverflow.chatoverflow.api.io.input.SampleInput;
      |import org.codeoverflow.chatoverflow.api.io.input.SerialInput;
      |import org.codeoverflow.chatoverflow.api.io.input.chat.DiscordChatInput;
      |import org.codeoverflow.chatoverflow.api.io.input.chat.MockUpChatInput;
      |import org.codeoverflow.chatoverflow.api.io.input.event.TipeeestreamEventInput;
      |import org.codeoverflow.chatoverflow.api.io.input.chat.TwitchChatInput;"""

  private def generateRequireMethods: String =
    """    /**
      |     *  Requires a twitch chat login that has to be created by the framework user.
      |     *
      |     * @param uniqueRequirementId any unique id by which your plugin can identify the requirement
      |     * @param displayName Is displayed to the framework user to tell him what to enter
      |     * @param isOptional true if this requirement is optional, false if mandatory
      |     * @return the requirement object
      |     */
      |    public Requirement<TwitchChatInput> twitchChat(String uniqueRequirementId, String displayName, boolean isOptional) {
      |        return requirements.requireInput(uniqueRequirementId, displayName, isOptional, TwitchChatInput.class);
      |    }
      |
      |    /**
      |     * Requires a connected mockup chat that has to be submitted by the user.
      |     *
      |     * @param uniqueRequirementId any unique id by which your plugin can identify the requirement
      |     * @param displayName Is displayed to the framework user and to tell him what to enter
      |     * @param isOptional true if this requirement is optional, false if mandatory
      |     * @return the requirement object
      |     */
      |    public Requirement<MockUpChatInput> mockupChat(String uniqueRequirementId, String displayName, boolean isOptional) {
      |        return requirements.requireInput(uniqueRequirementId, displayName, isOptional, MockUpChatInput.class);
      |    }
      |
      |    /**
      |     * Demonstration requirement for the sample input, to get a idea how requirements work
      |     *
      |     * @param uniqueRequirementId any unique id by which your plugin can identify the requirement
      |     * @param displayName Is displayed to the framework user and to tell him what to enter
      |     * @param isOptional true if this requirement is optional, false if mandatory
      |     * @return the requirement object
      |     */
      |    public Requirement<SampleInput> sampleInput(String uniqueRequirementId, String displayName, boolean isOptional) {
      |        return requirements.requireInput(uniqueRequirementId, displayName, isOptional, SampleInput.class);
      |    }
      |
      |    /**
      |     *  Requires a discord chat bot that has to be created by the framework user.
      |     *
      |     * @param uniqueRequirementId any unique id by which your plugin can identify the requirement
      |     * @param displayName Is displayed to the framework user to tell him what to enter
      |     * @param isOptional true if this requirement is optional, false if mandatory
      |     * @return the requirement object
      |     */
      |    public Requirement<DiscordChatInput> discordChat(String uniqueRequirementId, String displayName, boolean isOptional) {
      |        return requirements.requireInput(uniqueRequirementId, displayName, isOptional, DiscordChatInput.class);
      |    }
      |
      |    /**
      |     * Requires a connection with a device connected to a serial port (an Arduino for example)
      |     *
      |     * @param uniqueRequirementId any unique id by which your plugin can identify the requirement
      |     * @param displayName Is displayed to the framework user and to tell him what to enter
      |     * @param isOptional true if this requirement is optional, false if mandatory
      |     * @return the requirement object
      |     */
      |    public Requirement<SerialInput> serial(String uniqueRequirementId, String displayName, boolean isOptional) {
      |        return requirements.requireInput(uniqueRequirementId, displayName, isOptional, SerialInput.class);
      |    }
      |
      |    /**
      |     * Requires a login for the TipeeeStream api that has to be created by the framework user.
      |     *
      |     * @param uniqueRequirementId any unique id by which your plugin can identify the requirement
      |     * @param displayName Is displayed to the framework user and to tell him what to enter
      |     * @param isOptional true if this requirement is optional, false if mandatory
      |     * @return the requirement object
      |     */
      |    public Requirement<TipeeestreamEventInput> tipeeeStream(String uniqueRequirementId, String displayName, boolean isOptional) {
      |        return requirements.requireInput(uniqueRequirementId, displayName, isOptional, TipeeestreamEventInput.class);
      |    }
      |
      |    /**
      |     * @param uniqueRequirementId any unique id by which your plugin can identify the requirement
      |     * @param displayName Is displayed to the framework user and to tell him what to enter
      |     * @param isOptional true if this requirement is optional, false if mandatory
      |     * @return the requirement object
      |     */
      |    public Requirement<FileInput> file(String uniqueRequirementId, String displayName, boolean isOptional) {
      |        return requirements.requireInput(uniqueRequirementId, displayName, isOptional, FileInput.class);
      |    }
      |
      |    // Add more inputs here"""
}

object RequirementsFile {
  def apply(requirementsDirectory: File, fileName: String, files: Seq[AnnotatedRequirement]): RequirementsFile =
    new RequirementsFile(requirementsDirectory, fileName, files)
}