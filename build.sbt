// Main project config
organization := "org.codeoverflow"
name := "chatoverflow"
version := "4.0.0"

// Main class and sub projects
mainClass := Some("org.codeoverflow.chatoverflow.Launcher")
lazy val launcherProject = project in file("launcher")
lazy val buildProject = project in file("build") // needed for intelliJ support

// Global settings
inThisBuild(List(scalaVersion := "2.12.5"))

import org.codeoverflow.chatoverflow.build.BuildUtils

Global / javacOptions ++= BuildUtils.getJava8CrossOptions
Global / scalacOptions ++= Seq("-deprecation", "-feature")