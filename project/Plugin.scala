import java.io.File

/**
  * A plugin represents a directory in a plugin source directory. Every plugin has its own build file and source folder.
  *
  * @param pluginBasePath the plugin base source directory
  * @param name           the name of the plugin
  */
class Plugin(pluginBasePath: String, name: String) {
  val pluginDirectoryPath: String = s"$pluginBasePath/${toPluginPathName(name)}"

  /**
    * Creates the plugin folder inside of a plugin source directory
    *
    * @return true, if the process was successful
    */
  def createPluginFolder(): Boolean = {
    val pluginDirectory = new File(pluginDirectoryPath)

    if (pluginDirectory.exists()) {
      false
    } else {
      pluginDirectory.mkdir()
      true
    }
  }

  /**
    * Creates the plugin src folder inside of a plugin folder
    *
    * @note Make sure to create the plugin folder first!
    * @return true, if the process was successful
    */
  def createSrcFolder(): Boolean = {
    if (new File(s"$pluginDirectoryPath/src").mkdir() &&
      new File(s"$pluginDirectoryPath/src/main").mkdir() &&
      new File(s"$pluginDirectoryPath/src/main/scala").mkdir()) {
      true
    } else {
      false
    }
  }

  /**
    * Creates a simple sbt file with name and version info into the plugin folder
    *
    * @param version the version of the sbt plugin project
    * @return true, if the process was successful
    */
  def createSbtFile(version: String): Boolean = {
    val sbtFile = new SbtFile(name, version)
    sbtFile.save(pluginDirectoryPath)
  }

  private def toPluginPathName(name: String) = name.replace(" ", "").toLowerCase
}