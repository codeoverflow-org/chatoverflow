// ---------------------------------------------------------------------------------------------------------------------
// PROJECT INFORMATION
// ---------------------------------------------------------------------------------------------------------------------

/*
 * A brief introduction of the sbt related folder structure:
 *       root
 *       |  build.sbt
 *       |  plugins.sbt (produced by the fetch task)
 *       |  -> api project (required to build plugins and the framework)
 *       |  -> a plugin source directory
 *       |  -> -> a plugin folder = plugin
 *       |  -> -> -> build.sbt
 *       |  -> -> -> source etc.
 *       |  -> -> another folder = another plugin
 *       |  -> -> -> build.sbt
 *       |  -> -> -> source etc.
 *       |  -> another plugin source directory (optional)
 *       |  -> gui project (build will be skipped, if missing)
 *       |  -> bootstrap launcher (for end-user deployments)
 *       |  -> build project (contains code for all sbt tasks and sbt related things)
 */

name := "ChatOverflow"
version := "3.0.0"
mainClass := Some("org.codeoverflow.chatoverflow.Launcher")

// One version for all sub projects. Use "retrieveManaged := true" to download and show all library dependencies.
val scalaMajorVersion = "2.12"
val scalaMinorVersion = ".5"
inThisBuild(List(
  scalaVersion := s"$scalaMajorVersion$scalaMinorVersion",
  retrieveManaged := false)
)

import org.codeoverflow.chatoverflow.build.BuildUtils
javacOptions ++= BuildUtils.getJava8CrossOptions

// Link the launcher
lazy val launcherProject = project in file("launcher")

// not actually used. Just required to say IntelliJ to mark the build directory as a sbt project, otherwise it wouldn't detect it.
lazy val buildProject = project in file("build")

// ---------------------------------------------------------------------------------------------------------------------
// LIBRARY DEPENDENCIES
// ---------------------------------------------------------------------------------------------------------------------

// Command Line Parsing
libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0"

// log4j Logger
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.22"

// Scalatra (REST, ...)
libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % "2.6.5",
  "org.scalatra" %% "scalatra-scalate" % "2.6.5",
  "org.scalatra" %% "scalatra-specs2" % "2.6.5",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "provided",
  "org.eclipse.jetty" % "jetty-webapp" % "9.4.7.v20170914" % "provided",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "org.scalatra" %% "scalatra-json" % "2.6.3",
  "org.scalatra" %% "scalatra-swagger" % "2.6.5",
)

// JSON Lib (Jackson)
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.2"

// PIRCBotX
libraryDependencies += "org.pircbotx" % "pircbotx" % "2.1"

// Reflections API for annotation indexing
libraryDependencies += "org.reflections" % "reflections" % "0.9.11"

// Akka Actors
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.18",
  //"com.typesafe.akka" %% "akka-testkit" % "2.5.18" % Test
)

// Google GSON
libraryDependencies += "com.google.code.gson" % "gson" % "2.8.5"

// JDA
resolvers += "jcenter-bintray" at "https://jcenter.bintray.com"
libraryDependencies += "net.dv8tion" % "JDA" % "3.8.3_463"

// Serial Communication
libraryDependencies += "com.fazecast" % "jSerialComm" % "2.5.1"

// Socket.io
libraryDependencies += "io.socket" % "socket.io-client" % "1.0.0"

// Coursier
libraryDependencies += "io.get-coursier" %% "coursier" % "2.0.0-RC3-2"

// ---------------------------------------------------------------------------------------------------------------------
// PLUGIN FRAMEWORK DEFINITIONS
// ---------------------------------------------------------------------------------------------------------------------

// Plugin framework settings
lazy val pluginBuildFileName = settingKey[String]("The filename of the plugin build file. Remember to gitignore it!")
lazy val pluginFolderNames = settingKey[List[String]]("The folder names of all plugin source directories.")
lazy val pluginTargetFolderNames = settingKey[List[String]]("The folder names of compiled and packaged plugins. Remember to gitignore these!")
lazy val apiProjectPath = settingKey[String]("The path to the api sub project. Remember to gitignore it!")
lazy val guiProjectPath = settingKey[String]("The path of the Angular gui.")

// Plugin framework tasks
lazy val create = TaskKey[Unit]("create", "Creates a new plugin. Interactive command using the console.")
lazy val fetch = TaskKey[Unit]("fetch", "Searches for plugins in plugin directories, builds the plugin build file.")
lazy val copy = TaskKey[Unit]("copy", "Copies all packaged plugin jars to the target plugin folder.")
lazy val deploy = TaskKey[Unit]("deploy", "Prepares the environment for deployment, fills deploy folder.")
lazy val deployDev = TaskKey[Unit]("deployDev", "Prepares the environment for plugin developers, fills deployDev folder.")
lazy val gui = TaskKey[Unit]("gui", "Installs GUI dependencies and builds it using npm.")

pluginBuildFileName := "plugins.sbt"
pluginFolderNames := List("plugins-public", "plugins-private")
pluginTargetFolderNames := List("plugins", s"target/scala-$scalaMajorVersion/plugins")
apiProjectPath := "api"
guiProjectPath := "gui"


import org.codeoverflow.chatoverflow.build.GUIUtility
import org.codeoverflow.chatoverflow.build.deployment.DeploymentUtility
import org.codeoverflow.chatoverflow.build.plugins.{PluginCreateWizard, PluginUtility}

create := new PluginCreateWizard(streams.value.log).createPluginTask(pluginFolderNames.value, PluginCreateWizard.getApiVersion.value)
fetch := new PluginUtility(streams.value.log).fetchPluginsTask(pluginFolderNames.value, pluginBuildFileName.value,
  pluginTargetFolderNames.value, apiProjectPath.value)
copy := new PluginUtility(streams.value.log).copyPluginsTask(pluginFolderNames.value, pluginTargetFolderNames.value, scalaMajorVersion)
deploy := DeploymentUtility.prepareDeploymentTask(streams.value.log, scalaMajorVersion)
deployDev := DeploymentUtility.prepareDevDeploymentTask(streams.value.log, scalaMajorVersion, apiProjectPath.value, libraryDependencies.value.toList)
gui := new GUIUtility(streams.value.log).guiTask(guiProjectPath.value, streams.value.cacheDirectory / "gui")

Compile / packageBin := {
  new GUIUtility(streams.value.log).packageGUITask(guiProjectPath.value, crossTarget.value)
  (Compile / packageBin).value
}

Compile / unmanagedJars := new GUIUtility(streams.value.log).getGUIJarClasspath(guiProjectPath.value, crossTarget.value)

fork in run := true // Start ChatOverflow in it's own java process when starting it with 'sbt run'

// Clears the built GUI dir on clean
cleanFiles += baseDirectory.value / guiProjectPath.value / "dist"
