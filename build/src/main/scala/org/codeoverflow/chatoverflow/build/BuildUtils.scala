package org.codeoverflow.chatoverflow.build

import java.io.File

import sbt.Keys.scalaVersion
import sbt._
import sbt.internal.util.ManagedLogger

/**
 * Provides generic build related utilities.
 */
object BuildUtils {

  /**
   * This method can be used to create better readable sbt console output by declaring start and stop of a custom task.
   *
   * @param taskName the name of the task (use caps for better readability)
   * @param logger   the sbt logger of the task
   * @param task     the task itself
   */
  def withTaskInfo(taskName: String, logger: ManagedLogger)(task: => Unit): Unit = {

    // Info when task started (better log comprehension)
    logger info s"Started custom task: $taskName"

    // Doing the actual work
    task

    // Info when task stopped (better log comprehension)
    logger info s"Finished custom task: $taskName"
  }

  /**
   * Creates a file listing with all files including files in any sub-dir.
   *
   * @param dir the directory for which the file listing needs to be created.
   * @return the file listing as a set of files.
   */
  def getAllDirectoryChilds(dir: File): Set[File] = {
    val dirEntries = dir.listFiles()
    (dirEntries.filter(_.isFile) ++ dirEntries.filter(_.isDirectory).flatMap(getAllDirectoryChilds)).toSet
  }

  /**
   * Checks whether the current os is windows.
   *
   * @return true if running on any windows version, false otherwise
   */
  def isRunningOnWindows: Boolean = System.getProperty("os.name").toLowerCase().contains("win")

  /**
   * Returns the used Java version, e.g. 1.8 or 10.
   */
  def getJavaVersion: Double = {
    System.getProperty("java.specification.version").toDouble
  }

  /**
   * Returns required javac options to compile Java sources against Java 8.
   * This ensures that the developer can use a newer Java version and the produced class files are still executable
   * by end-users that have a older Java version e.g. Java 8. (Java 8 is minimum)
   * @return
   */
  def getJava8CrossOptions: Seq[String] = {
    // --release flag didn't exist in those versions yet, Java 8 doesn't need any cross-compiling options
    // and lower versions than 8 aren't supported anyway.
    if (getJavaVersion < 9)
      Seq()
    else
      Seq("--release", "8") // please compile against Java 8
  }

  /**
   * Creates MavenRepository for GPR with the given github repository.
   * The default user is the CodeOverflow organisation but this can be overwritten with the GITHUB_OWNER env var.
   *
   * @param githubRepository the repository to which you want to publish
   */
  def publishToGPR(githubRepository: String): Some[MavenRepository] = {
    val githubOwner = sys.env.getOrElse("GITHUB_OWNER", "codeoverflow-org")

    Some(s"GitHub Package Registry" at s"https://maven.pkg.github.com/$githubOwner/$githubRepository")
  }

  private val isSnapshot = sys.env.getOrElse("PUBLISH_SNAPSHOT", "false").trim.toLowerCase == "true"

  /**
   * Returns the passed version with a -SNAPSHOT suffix when the "PUBLISH_SNAPSHOT" environment variable is set to true.
   * This variable gets set by the GitHub Actions which is responsible for publishing the maven packages.
   * Otherwise it will return the normal version for use in a normal dev environment or on an actual release.
   *
   * @param version the version of a sbt project
   * @return the version with an SNAPSHOT suffix depending on the environment
   */
  def dynamicSnapshotVersion(version: String): String = {
    if (isSnapshot) {
      s"$version-SNAPSHOT"
    } else {
      version
    }
  }

  /**
   * Returns the major scala version which this version of sbt is using.
   */
  def getSbtScalaVersion: String = scala.util.Properties.versionNumberString.split('.').dropRight(1).mkString(".")

  /**
   * A task that returns the major and minor version of the currently used version of scala, e.g. 2.12.
   */
  lazy val scalaMajorVersion: Def.Initialize[Task[String]] = Def.task {
    scalaVersion.value.split('.').dropRight(1).mkString(".")
  }
}
