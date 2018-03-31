// ---------------------------------------------------------------------------------------------------------------------
// PROJECT INFORMATION
// ---------------------------------------------------------------------------------------------------------------------

name := "ChatOverflow"
version := "0.1"
mainClass := Some("org.codeoverflow.chatoverflow.ChatOverflow")

// One version for all sub projects. Use "retrieveManaged := true" to download and show all library dependencies.
inThisBuild(List(
  scalaVersion := "2.12.5",
  retrieveManaged := false)
)





// ---------------------------------------------------------------------------------------------------------------------
// LIBRARY DEPENDENCIES
// ---------------------------------------------------------------------------------------------------------------------

// Command Line Parsing Dependencies
libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0"





// ---------------------------------------------------------------------------------------------------------------------
// PLUGIN FRAMEWORK DEFINITIONS
// ---------------------------------------------------------------------------------------------------------------------

// Plugin framework settings
lazy val pluginBuildFile = settingKey[String]("The filename of the plugin build file. Remember to gitignore it!")
lazy val pluginFolders = settingKey[List[String]]("The folder names of all plugin source directories.")
lazy val pluginTargetFolders = settingKey[List[String]]("The folder names of compiled and packaged plugins. Remember to gitignore these!")

// Plugin framework tasks
lazy val create = TaskKey[Unit]("create", "Creates a new plugin. Interactive command using the console.")
lazy val fetch = TaskKey[Unit]("fetch", "Searches for plugins in plugin directories, builds the plugin build file.")
lazy val copy = TaskKey[Unit]("copy", "Copies all packaged plugin jars to the target plugin folder.")

pluginBuildFile := "plugins.sbt"
pluginFolders := List("plugins-public")
pluginTargetFolders := List("plugins")





// ---------------------------------------------------------------------------------------------------------------------
// PLUGIN FRAMEWORK IMPLEMENTATION
// ---------------------------------------------------------------------------------------------------------------------

// Plugin reload task
fetch := {

  // TODO: Implement

}

// Plugin create task
create := {

  // TODO: Implement

}

// Plugin copy task
copy := {

  // TODO: Implement

}