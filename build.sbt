// ---------------------------------------------------------------------------------------------------------------------
// PROJECT INFORMATION
// ---------------------------------------------------------------------------------------------------------------------

name := "ChatOverflow"
version := "0.2"
mainClass := Some("org.codeoverflow.chatoverflow.Launcher")

// One version for all sub projects. Use "retrieveManaged := true" to download and show all library dependencies.
val scalaMajorVersion = "2.12"
val scalaMinorVersion = ".5"
inThisBuild(List(
  scalaVersion := s"$scalaMajorVersion$scalaMinorVersion",
  retrieveManaged := false)
)

// Link the bootstrap launcher
lazy val bootstrapProject = project in file("bootstrap")

// ---------------------------------------------------------------------------------------------------------------------
// LIBRARY DEPENDENCIES
// ---------------------------------------------------------------------------------------------------------------------

// Command Line Parsing
libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0"

// log4j Logger
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.22"

// Scalatra (REST, ...)
libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % "2.6.+",
  "org.scalatra" %% "scalatra-scalate" % "2.6.+",
  "org.scalatra" %% "scalatra-specs2" % "2.6.+",
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
resolvers += "jcenter-bintray" at "http://jcenter.bintray.com"
libraryDependencies += "net.dv8tion" % "JDA" % "4.ALPHA.0_82"

//Serial Communication
libraryDependencies += "com.fazecast" % "jSerialComm" % "[2.0.0,3.0.0)"

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
lazy val bs = TaskKey[Unit]("bs", "Updates the bootstrap project with current dependencies and chat overflow jars.")
lazy val deploy = TaskKey[Unit]("deploy", "Prepares the environment for deployment, fills deploy folder.")

pluginBuildFileName := "plugins.sbt"
pluginFolderNames := List("plugins-public")
pluginTargetFolderNames := List("plugins", s"target/scala-$scalaMajorVersion/plugins")
apiProjectPath := "api"

create := BuildUtility(streams.value.log).createPluginTask(pluginFolderNames.value)
fetch := BuildUtility(streams.value.log).fetchPluginsTask(pluginFolderNames.value, pluginBuildFileName.value,
  pluginTargetFolderNames.value, apiProjectPath.value)
copy := BuildUtility(streams.value.log).copyPluginsTask(pluginFolderNames.value, pluginTargetFolderNames.value, scalaMajorVersion)
bs := BootstrapUtility.bootstrapGenTask(streams.value.log, s"$scalaMajorVersion$scalaMinorVersion")
deploy := BootstrapUtility.prepareDeploymentTask(streams.value.log, scalaMajorVersion)