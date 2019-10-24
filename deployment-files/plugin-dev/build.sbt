// This is a stripped down version of the build.sbt found in the main framework.
// Its almost like the one from the framework but without some sbt tasks like 'deploy' that aren't needed for plugin developers
// and without dependencies, because those get their own generated file.

// ---------------------------------------------------------------------------------------------------------------------
// PROJECT INFORMATION
// ---------------------------------------------------------------------------------------------------------------------

name := "ChatOverflow"
version := "3.0.0"

// This task has to be overwritten, because sbt will only look in source files for main classes, but the class is in a jar.
Compile / discoveredMainClasses := Seq("org.codeoverflow.chatoverflow.Launcher")

// One version for all sub projects. Use "retrieveManaged := true" to download and show all library dependencies.
val scalaMajorVersion = "2.12"
val scalaMinorVersion = ".5"
inThisBuild(List(
  scalaVersion := s"$scalaMajorVersion$scalaMinorVersion",
  retrieveManaged := false)
)

unmanagedBase := file("bin")

// ---------------------------------------------------------------------------------------------------------------------
// PLUGIN FRAMEWORK DEFINITIONS
// ---------------------------------------------------------------------------------------------------------------------

// Plugin framework settings
lazy val pluginBuildFileName = settingKey[String]("The filename of the plugin build file. Remember to gitignore it!")
lazy val pluginFolderNames = settingKey[List[String]]("The folder names of all plugin source directories.")
lazy val pluginTargetFolderNames = settingKey[List[String]]("The folder names of compiled and packaged plugins. Remember to gitignore these!")
lazy val apiProjectPath = settingKey[String]("The path to the api sub project. Remember to gitignore it!")

// Plugin framework tasks
lazy val create = TaskKey[Unit]("create", "Creates a new plugin. Interactive command using the console.")
lazy val fetch = TaskKey[Unit]("fetch", "Searches for plugins in plugin directories, builds the plugin build file.")
lazy val copy = TaskKey[Unit]("copy", "Copies all packaged plugin jars to the target plugin folder.")

pluginBuildFileName := "plugins.sbt"
pluginFolderNames := List("plugins-public", "plugins-private")
pluginTargetFolderNames := List("plugins", s"target/scala-$scalaMajorVersion/plugins")
apiProjectPath := "api"


import org.codeoverflow.chatoverflow.build.plugins.{PluginCreateWizard, PluginUtility}

create := new PluginCreateWizard(streams.value.log).createPluginTask(pluginFolderNames.value, PluginCreateWizard.getApiVersion.value)
fetch := new PluginUtility(streams.value.log).fetchPluginsTask(pluginFolderNames.value, pluginBuildFileName.value,
  pluginTargetFolderNames.value, apiProjectPath.value)
copy := new PluginUtility(streams.value.log).copyPluginsTask(pluginFolderNames.value, pluginTargetFolderNames.value, scalaMajorVersion)

packageBin / includePom := false
fork in run := true // Start ChatOverflow in it's own java process when starting it with 'sbt run'