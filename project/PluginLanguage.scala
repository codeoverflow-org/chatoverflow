object PluginLanguage extends Enumeration {
  val JAVA, SCALA = Value

  /**
    * Parses the string into a instance of PluginLanguage with the same name.
    * Note that this method is not case-sensitive.
    *
    * @param s the string to parse
    * @return the value of the enum or None if no value with the passed name could be found
    */
  def fromString(s: String): Option[PluginLanguage.Value] = {
    PluginLanguage.values.find(v => v.toString.toLowerCase == s.toLowerCase)
  }

  /**
    * Converts the enum instance into a source file for a basic plugin in that language.
    *
    * @param name the name of the plugin, used to generate the class name and some log statements.
    * @param lang the instance of the enum, representatives the language, in which the source file needs to be.
    * @return
    */
  def getSourceFileContent(name: String, lang: PluginLanguage.Value): String = lang match {
    case JAVA =>
      s"""
        |import org.codeoverflow.chatoverflow.api.io.input.SampleInput;
        |import org.codeoverflow.chatoverflow.api.plugin.PluginImpl;
        |import org.codeoverflow.chatoverflow.api.plugin.PluginManager;
        |import org.codeoverflow.chatoverflow.api.plugin.configuration.Requirement;
        |
        |public class ${name}Plugin extends PluginImpl {
        |
        |    public ${name}Plugin(PluginManager manager) {
        |        super(manager);
        |    }
        |
        |    // require more requirements as needed here
        |    private Requirement<SampleInput> sampleReq = require.input.sampleInput("sampleReq", "Sample requirement", true);
        |
        |    /**
        |     * The setup method is executed one, when the plugin is started. Do NOT define your requirements in here!
        |     */
        |    @Override
        |    public void setup() {
        |        // you can adjust the loop interval here
        |        // loopInterval = 1000;
        |
        |        log("Initialized $name plugin!");
        |    }
        |
        |    /**
        |     * The loop method is executed in loop with a specified interval until the shutdown method is called.
        |     * The loop method is NOT executed if a negative loop interval is set.
        |     */
        |    @Override
        |    public void loop() {
        |        log("$name plugin loop!");
        |    }
        |
        |    /**
        |     * The shutdown method should contain logic to close everything.
        |     */
        |    @Override
        |    public void shutdown() {
        |        log("Shutting down $name plugin!");
        |    }
        |}
        |
      """.stripMargin
    case SCALA =>
      s"""
        |import org.codeoverflow.chatoverflow.api.plugin.{PluginImpl, PluginManager}
        |
        |class ${name}Plugin(manager: PluginManager) extends PluginImpl(manager) {
        |
        |  // require more requirements as needed here
        |  private val sampleReq = require.input.sampleInput("sampleReq", "Sample requirement", true)
        |
        |  // you can adjust the loop interval here
        |  // loopInterval = 1000;
        |
        |  /**
        |    * The setup method is executed one, when the plugin is started. Do NOT define your requirements in here!
        |    */
        |  override def setup(): Unit = {
        |    log("Initialized $name plugin!")
        |  }
        |
        |  /**
        |    * The loop method is executed in loop with a specified interval until the shutdown method is called.
        |    * The loop method is NOT executed if a negative loop interval is set.
        |    */
        |  override def loop(): Unit = {
        |    log("$name plugin loop!")
        |  }
        |
        |  /**
        |    * The shutdown method should contain logic to close everything.
        |    */
        |  override def shutdown(): Unit = {
        |    log("Shutting down $name plugin!")
        |  }
        |}
        |
      """.stripMargin
  }
}
