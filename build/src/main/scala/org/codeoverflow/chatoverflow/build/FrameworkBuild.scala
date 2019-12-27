package org.codeoverflow.chatoverflow.build

import sbt._
import sbt.internal.BuildDef

/**
 * Configures the gui and api dependencies and aggregations of the framework sbt project.
 * This is a fallback to allow compiling and running when the plugins.sbt hasn't been fetched yet.
 * If a plugins.sbt exists it has priority because sbt first applies the build project and then all *.sbt files meaning
 * that the config in plugins.sbt overrides this config when it exists, otherwise this configuration is used.
 */
object FrameworkBuild extends BuildDef {
  private val guiProjectDirectory = file("gui")
  lazy val guiProject: Option[Project] = {
    if (guiProjectDirectory.exists())
      Some(project in guiProjectDirectory)
    else
      None
  }

  lazy val apiProject = project in file("api")

  lazy val root = {
    val frameworkWithApi = (project in file(".") withId "root")
      .dependsOn(apiProject).aggregate(apiProject)

    guiProject match {
      // This % "runtime" says sbt that the gui is only a dependency at runtime and that it can compile
      // the framework and the gui in parallel.
      case Some(gui) => frameworkWithApi.dependsOn(gui % "runtime").aggregate(gui)
      case None => frameworkWithApi
    }
  }

  override def projects: Seq[Project] = {
    Seq(root, apiProject) ++ guiProject
  }
}
