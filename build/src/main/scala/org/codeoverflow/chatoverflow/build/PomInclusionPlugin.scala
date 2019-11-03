package org.codeoverflow.chatoverflow.build

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

/**
 * A sbt plugin to automatically include the dependencies of a sbt project in the jar as a pom file called "dependencies.pom".
 */
object PomInclusionPlugin extends AutoPlugin {

  // Everything in autoImport will be visible to sbt project files
  // They can set this value to false if they don't want to include their dependencies as a pom file
  object autoImport {
    val includePom = settingKey[Boolean]("Whether to include a pom file inside the jar with all dependencies.")
  }
  import autoImport._

  // We require to have the Compile configuration and the packageBin task to override
  override def requires = JvmPlugin
  override def trigger = allRequirements

  // Adds our custom task before the packageBin task
  override val projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(Seq(
      Compile / packageBin := (Compile / packageBin).dependsOn(addPomToOutput).value
    ))

  // Sets default values
  override def buildSettings: Seq[Def.Setting[_]] = inConfig(Compile)(
    includePom in packageBin := true
  )

  // Just copies the pom resulted by makePom into the directory for compiled classes
  // That way the file will be included in the jar
  private lazy val addPomToOutput = Def.taskDyn {
    if ((includePom in packageBin).value) Def.task {
      val pomFile = (Compile / makePom).value

      IO.copyFile(pomFile, new File((Compile / classDirectory).value, "dependencies.pom"))
    } else
      Def.task {} // if disabled, do nothing
  }
}