import java.io.{BufferedWriter, File, FileWriter, IOException}

/**
  * Represents a simple sbt files content and methods to create a new sbt file. Not intended to open/read sbt files.
  *
  * @param name              the name of a sbt project
  * @param version           the version of a sbt project
  * @param pluginDirectories list of paths of sub projects
  * @param apiProjectPath    the path of a base api project which every project depends on
  * @param defineRoot        true, if a root project (".") should be defined in the sbt file
  */
class SbtFile(var name: String, var version: String, var pluginDirectories: List[File], var apiProjectPath: String, var defineRoot: Boolean) {
  /**
    * Represents a simple sbt files content and methods to create a new sbt file. Not intended to open/read sbt files.
    *
    * @param name    the name of a sbt project
    * @param version the version of a sbt project
    */
  def this(name: String, version: String) = this(name, version, List(), "", false)

  /**
    * Represents a simple sbt files content and methods to create a new sbt file. Not intended to open/read sbt files.
    */
  def this() = this("", "")

  /**
    * Tries to save the sbt files content into a defined directory.
    *
    * @param path the directory of the sbt file
    * @return true, if the save process was successful
    */
  def save(path: String): Boolean = {

    // TODO: Check if build.sbt can be named $pluginName.sbt
    val buildFile = new File(s"$path/build.sbt")

    // Write the build file using the SbtFiles string representation
    val writer = new BufferedWriter(new FileWriter(buildFile))
    try {
      writer.write(this.toString)
      true
    } catch {
      case _: IOException => false
    } finally {
      writer.close()
    }
  }

  /**
    * Returns the string representation of the sbt files content in valid sbt/scala syntax
    *
    * @return a multiline string with all defined attributes
    */
  override def toString: String = {

    val sbtContent = new StringBuilder("// GENERATED FILE USING THE CHAT OVERFLOW PLUGIN FRAMEWORK")

    if (name != "") {
      sbtContent append "\nname := \"%s\"".format(name)
    }

    if (version != "") {
      sbtContent append "\nversion := \"%s\"".format(version)
    }

    // TODO: Implement pluginDirectories, root, apiProject

    sbtContent.mkString
  }
}
