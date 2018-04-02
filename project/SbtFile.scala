import java.io.{BufferedWriter, File, FileWriter, IOException}

class SbtFile(var name: String, var version: String, var pluginDirectories: List[File], var apiProjectPath: String, var defineRoot: Boolean) {
  def this(name: String, version: String) = this(name, version, List(), "", false)

  def this() = this("", "")

  def save(pluginPath: String): Boolean = {

    // TODO: Check if build.sbt can be named $pluginName.sbt
    val buildFile = new File(s"$pluginPath/build.sbt")

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
