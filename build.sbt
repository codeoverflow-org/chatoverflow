import org.codeoverflow.chatoverflow.build.BuildUtils

// Main project config
organization := "org.codeoverflow"
name := "chatoverflow"
version := BuildUtils.dynamicSnapshotVersion("4.0.0")

// Main class and sub projects
mainClass := Some("org.codeoverflow.chatoverflow.Launcher")
lazy val launcherProject = project in file("launcher")
lazy val buildProject = project in file("build") // needed for intelliJ support

// Global settings
Global / javacOptions ++= BuildUtils.getJava8CrossOptions
Global / scalacOptions ++= Seq("-deprecation", "-feature")