// Main project config
name := "ChatOverflow"
version := "4.0.0"

// Main class and sub projects
mainClass := Some("org.codeoverflow.chatoverflow.Launcher")
lazy val launcherProject = project in file("launcher")
lazy val buildProject = project in file("build") // needed for intelliJ support

lazy val apiProject = project in file("api")
lazy val guiProject = project in file("gui")

// This is a fallback to allow compiling and running when the plugins.sbt hasn't fetched yet.
// Sbt applies all .sbt files in alphabetical order so this sets the base dependencies of the framework
// and if an plugins.sbt is present it will overwrite it with the plugins plus the same ones as here (api, gui).
// For alphabetical ordering see https://github.com/sbt/sbt/blob/3fc9513ec5c53b1385a6e95bf52d4556a47e2448/main/src/main/scala/sbt/internal/Load.scala#L1116
lazy val root = (project in file(".")).dependsOn(apiProject, guiProject % "runtime->compile").aggregate(apiProject, guiProject)

// Settings
inThisBuild(List(scalaVersion := "2.12.5"))

import org.codeoverflow.chatoverflow.build.BuildUtils

Global / javacOptions ++= BuildUtils.getJava8CrossOptions
fork in run := true // Start ChatOverflow in it's own java process when starting it with 'sbt run'