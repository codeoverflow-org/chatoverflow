package org.codeoverflow.chatoverflow.build

import java.io.{BufferedWriter, File, FileWriter, IOException}

import org.codeoverflow.chatoverflow.build.plugins.Plugin

/**
 * Represents a simple sbt files content and methods to create a new sbt file. Not intended to open/read sbt files.
 *
 * @param name            the name of a sbt project
 * @param version         the version of a sbt project
 * @param plugins         list of paths of sub projects
 * @param apiProjectPath  the path of a base api project which every project depends on
 * @param guiProjectPath  the path of the gui project
 * @param defineRoot      true, if a root project (".") should be defined in the sbt file
 * @param excludeScalaLib true, if the scala library and library version suffix should be disabled
 */
class SbtFile(val name: String, val version: String, val plugins: List[Plugin], val apiProjectPath: String,
              val guiProjectPath: String, val defineRoot: Boolean, excludeScalaLib: Boolean = false) {
  /**
   * Represents a simple sbt files content and methods to create a new sbt file. Not intended to open/read sbt files.
   *
   * @param name    the name of a sbt project
   * @param version the version of a sbt project
   */
  def this(name: String, version: String) = this(name, version, List(), "", "", false)

  /**
   * Represents a simple sbt files content and methods to create a new sbt file. Not intended to open/read sbt files.
   */
  def this() = this("", "")

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
        if(plugin.isScala) {
          pluginLine += ".dependsOn(apiScalaProject)"
        }
      }

      sbtContent append pluginLine
    }

    if (apiProjectPath != "") {
      sbtContent append "\n\nlazy val apiProject = project in file(\"%s\")".format(apiProjectPath)
      sbtContent append "\nlazy val apiScalaProject = (project in file(\"%s/scala\")).dependsOn(apiProject)".format(apiProjectPath)
    }
    if (guiProjectPath != "") {
      sbtContent append "\nlazy val guiProject = project in file(\"%s\")".format(guiProjectPath)
    }

    if (defineRoot) {
      var rootLine = "\n\nlazy val root = (project in file(\".\")).aggregate(%s)"
        .format(plugins.map(p => s"`${p.normalizedName}`").mkString(", "))

      if (apiProjectPath != "") {
        rootLine += ".dependsOn(apiProject, apiScalaProject).aggregate(apiProject, apiScalaProject)"
      }
      if (guiProjectPath != "") {
        // This % "runtime" says sbt that the gui is only a dependency at runtime and that it can compile
        // the framework and the gui in parallel.
        rootLine += ".dependsOn(guiProject % \"runtime\").aggregate(guiProject)"
      }

      sbtContent append rootLine
    }

    if (excludeScalaLib) {
      sbtContent append
        """
          |// Excludes the Scala library
          |// Remove these two lines if you now wish to use Scala
          |autoScalaLibrary := false
          |crossPaths := false
          |""".stripMargin
    }

    sbtContent.mkString
  }
}
