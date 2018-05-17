import java.io.File

/**
  * A plugin represents a directory in a plugin source directory. Every plugin has its own build file and source folder.
  *
  * @param pluginSourceDirectoryName the plugin base source directory
  * @param name                      the name of the plugin
  */
class Plugin(val pluginSourceDirectoryName: String, val name: String) {
  val normalizedName: String = Plugin.toPluginPathName(name)
  val pluginDirectoryPath: String = s"$pluginSourceDirectoryName/$normalizedName"

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

    // TODO: Check if build.sbt can be named $pluginName.sbt
    sbtFile.save(s"$pluginDirectoryPath/build.sbt")
  }

  /**
    * Fetches the build plugin jar from the target folder of a given scala version.
    *
    * @param scalaMajorVersion the major scala version (x.x)
    * @return a seq of found jar files or a empty seq (e.g. if the target folder is not found)
    */
  def getBuildPluginFiles(scalaMajorVersion: String): Seq[File] = {

    val pluginTargetFolder = new File(s"$pluginDirectoryPath/target/scala-$scalaMajorVersion")

    if (!pluginTargetFolder.exists()) {
      Seq[File]()
    } else {
      pluginTargetFolder.listFiles().filter(_.getName.endsWith(".jar"))
    }
  }

}

object Plugin {

  /**
    * Checks if the plugin source folder is already used to store build jar plugin files.
    *
    * @param pluginSourceFolderName  the name of the source folder to test
    * @param pluginTargetFolderNames all names of target plugin folders
    * @return true, if the plugin source folder name does NOT exist in the plugin target folder list
    */
  def isSourceFolderNameValid(pluginSourceFolderName: String, pluginTargetFolderNames: List[String]): Boolean =
    !pluginTargetFolderNames.map(_.toLowerCase).contains(pluginSourceFolderName.toLowerCase)

  /**
    * Returns a list of all plugins in a given plugin source folder.
    *
    * @param pluginSourceFolderName the defined source folder of plugins
    * @return a seq of plugins
    */
  def getPlugins(pluginSourceFolderName: String): Seq[Plugin] = {
    val pluginSourceFolder = new File(pluginSourceFolderName)
    pluginSourceFolder.listFiles.filter(_.isDirectory).filter(_.getName != ".git")
      .map(folder => new Plugin(pluginSourceFolderName, folder.getName))

  }

  /**
    * Checks, if a plugin source folder exsists
    *
    * @param pluginSourceFolderName the source folder name
    * @return true, if exists and is directory
    */
  def sourceFolderExists(pluginSourceFolderName: String): Boolean = {
    val pluginSourceFolder = new File(pluginSourceFolderName)
    pluginSourceFolder.exists() && pluginSourceFolder.isDirectory
  }

  private def toPluginPathName(name: String) = name.replace(" ", "").toLowerCase

}


