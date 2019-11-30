// Main project config
name := "ChatOverflow"
version := "4.0.0"

// Main class and sub projects
mainClass := Some("org.codeoverflow.chatoverflow.Launcher")
lazy val launcherProject = project in file("launcher")
lazy val buildProject = project in file("build") // needed for intelliJ support

// Settings
inThisBuild(List(scalaVersion := "2.12.5"))

import org.codeoverflow.chatoverflow.build.BuildUtils

javacOptions ++= BuildUtils.getJava8CrossOptions
fork in run := true // Start ChatOverflow in it's own java process when starting it with 'sbt run'