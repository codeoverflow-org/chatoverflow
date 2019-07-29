import java.io.{BufferedWriter, File, FileWriter, IOException}

import sbt.librarymanagement.{CrossVersion, ModuleID}

/**
 * Represents a simple sbt files content and methods to create a new sbt file. Not intended to open/read sbt files.
 *
 * @param name           the name of a sbt project
 * @param version        the version of a sbt project
 * @param plugins        list of paths of sub projects
 * @param apiProjectPath the path of a base api project which every project depends on
 * @param defineRoot     true, if a root project (".") should be defined in the sbt file
 * @param dependencies   library dependencies to add to the sbt file
 */
class SbtFile(val name: String, val version: String, val plugins: List[Plugin], val apiProjectPath: String,
              val defineRoot: Boolean, dependencies: List[ModuleID]) {
  /**
    * Represents a simple sbt files content and methods to create a new sbt file. Not intended to open/read sbt files.
    *
    * @param name    the name of a sbt project
    * @param version the version of a sbt project
    */
  def this(name: String, version: String) = this(name, version, List(),  "", false, List())

  /**
    * Represents a simple sbt files content and methods to create a new sbt file. Not intended to open/read sbt files.
    */
  def this() = this("", "")

  /**
   * Represents a simple sbt files content and methods to create a new sbt file. Not intended to open/read sbt files.
   *
   * @param dependencies library dependencies to add to the sbt file
   */
  def this(dependencies: List[ModuleID]) = this("", "", List(), "", false, dependencies)

  /**
    * Tries to save the sbt files content into a defined directory.
    *
    * @param pathAndFileName the path of the sbt file (incl. file name)
    * @return true, if the save process was successful
    */
  def save(pathAndFileName: String): Boolean = {

    val buildFile = new File(pathAndFileName)

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

    val sbtContent = new StringBuilder("// GENERATED FILE USING THE CHAT OVERFLOW PLUGIN FRAMEWORK\n")

    if (name != "") {
      sbtContent append "\nname := \"%s\"".format(name.replaceAll("\\\\", ""))
    }

    if (version != "") {
      sbtContent append "\nversion := \"%s\"".format(version.replaceAll("\\\\", ""))
    }

    for (plugin <- plugins) {
      var pluginLine = "\nlazy val `%s` = (project in file(\"%s\"))".format(plugin.normalizedName, plugin.pluginDirectoryPath)

      if (apiProjectPath != "") {
        pluginLine += ".dependsOn(apiProject)"
      }

      sbtContent append pluginLine
    }

    if (apiProjectPath != "") {
      sbtContent append "\n\nlazy val apiProject = project in file(\"%s\")".format(apiProjectPath)
    }

    if (defineRoot) {
      var aggregateElems = plugins.map(p => s"`${p.normalizedName}`")
      if (apiProjectPath != "") {
        aggregateElems = "apiProject" +: aggregateElems
      }

      var rootLine = "\n\nlazy val root = (project in file(\".\")).aggregate(%s)"
        .format(aggregateElems.mkString(", "))

      if (apiProjectPath != "") {
        rootLine += ".dependsOn(apiProject)"
      }

      sbtContent append rootLine
    }

    if (dependencies.nonEmpty) {
      sbtContent append "\nresolvers += \"jcenter-bintray\" at \"http://jcenter.bintray.com\"\n"

      // Note that the %% in the string are required to escape the string formatter and will turn into a single %
      val depString = dependencies.map(m => {
        var formatString = ""

        if (m.crossVersion == CrossVersion.binary)
          formatString += "\"%s\" %%%% \"%s\" %% \"%s\""
        else
          formatString += "\"%s\" %% \"%s\" %% \"%s\""

        if (m.configurations.isDefined)
          formatString += " %% \"%s\""

        formatString.format(m.organization, m.name, m.revision, m.configurations.getOrElse(""))
      }).mkString("  ", ",\n  ", "")

      sbtContent append s"libraryDependencies ++= Seq(\n$depString\n)\n"
    }

    sbtContent.mkString
  }
}
