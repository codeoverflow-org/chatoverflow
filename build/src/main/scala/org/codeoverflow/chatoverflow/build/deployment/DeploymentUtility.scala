package org.codeoverflow.chatoverflow.build.deployment

import java.io.File
import java.nio.file.{Files, Paths}

import org.codeoverflow.chatoverflow.build.BuildUtils.withTaskInfo
import org.codeoverflow.chatoverflow.build.SbtFile
import sbt.internal.util.ManagedLogger
import sbt.librarymanagement.ModuleID

/**
 * Holds the functionality to create end-user and plugin dev deployments.
 * Should be used once for every new public version of chat overflow.
 */
object DeploymentUtility {

  /**
   * Prepares the environment for deployment. Should be called after package and assembly task.
   *
   * @param logger              the sbt logger
   * @param scalaLibraryVersion the scala library major version
   */
  def prepareDeploymentTask(logger: ManagedLogger, scalaLibraryVersion: String): Unit = {
    // Assuming, before this: clean, gui, bs, bootstrapProject/assembly, package
    // Assuming: Hardcoded "bin/" and "deploy/" folders
    // Assuming: A folder called "deployment-files/end-user/" with all additional files (license, bat, etc.)

    withTaskInfo("PREPARE DEPLOYMENT", logger) {

      logger info "Started deployment process."

      // First step: Create directory
      createOrEmptyFolder("deploy/")

      // Second step: Create bin directories and copy all binaries
      val targetJarDirectories = List("bin", "deploy/bin")
      prepareBinDirectories(logger, targetJarDirectories, scalaLibraryVersion, copyApi = true)

      // Third step: Copy bootstrap launcher
      copyJars(s"bootstrap/launcher/target/scala-$scalaLibraryVersion/", List("deploy/bin/"), logger)
      copyJars(s"bootstrap/updater/target/scala-$scalaLibraryVersion/", List("deploy/"), logger)

      // Last step: Copy additional files
      logger info "Copying additional deployment files..."
      val deploymentFiles = new File("deployment-files/end-user/")
      if (!deploymentFiles.exists()) {
        logger warn "Unable to find deployment files."
      } else {
        sbt.IO.copyDirectory(deploymentFiles, new File("deploy/"))
        logger info s"Finished copying additional deployment files."
      }
    }
  }

  /**
   * Prepares the environment for a deployment for plugin developers.
   * Should be called after package and apiProject/packagedArtifacts task.
   *
   * @param logger              the sbt logger
   * @param scalaLibraryVersion the scala library major version
   * @param apiProjectPath      the path to the api project. Used to copy the api into the deployDev directory
   * @param dependencies        the dependencies of the framework. Used to create a sbt file with them.
   */
  def prepareDevDeploymentTask(logger: ManagedLogger, scalaLibraryVersion: String, apiProjectPath: String, dependencies: List[ModuleID]): Unit = {
    // Assuming, before this: clean, gui, package and buildProject/package
    // Assuming: Hardcoded "bin/", "deployDev/" and "build/" folders
    // Assuming: A folder called "deployment-files/plugin-dev/" with more additional files for plugin developers

    withTaskInfo("PREPARE DEV DEPLOYMENT", logger) {

      logger info "Started deployment process for plugin dev environment."

      // First step: Create directory
      createOrEmptyFolder("deployDev/")

      // Second step: Copy framework, GUI and build-code jars
      val targetJarDirectories = List("bin", "deployDev/bin")
      prepareBinDirectories(logger, targetJarDirectories, scalaLibraryVersion, copyApi = false)

      createOrEmptyFolder("deployDev/project/lib")
      val buildCodeTargetDirectories = List("bin", "deployDev/project/lib")
      copyJars(s"build/target/scala-$scalaLibraryVersion/sbt-1.0", buildCodeTargetDirectories, logger)

      // Third step: Copy the api
      sbt.IO.copyDirectory(new File(apiProjectPath), new File("deployDev/api/"))
      sbt.IO.delete(new File("deployDev/api/target")) // otherwise compiled code would end up in the zip

      // Fourth step: Create sbt files containing all dependencies
      val depFile = new SbtFile(dependencies)
      sbt.IO.write(new File("deployDev/dependencies.sbt"), depFile.toString)

      // Last step: Copy additional files
      val devDeploymentFiles = new File("deployment-files/plugin-dev/")
      if (!devDeploymentFiles.exists()) {
        logger warn "Unable to find dev deployment files."
      } else {
        sbt.IO.copyDirectory(devDeploymentFiles, new File("deployDev/"))
        logger info "Finished copying additional dev deployment files."
      }
    }
  }

  private def prepareBinDirectories(logger: ManagedLogger, targetDirs: List[String], scalaLibraryVersion: String, copyApi: Boolean): Unit = {
    // First prepare all bin folders
    targetDirs.foreach(d => {
      logger info s"Preparing '$d' folder."
      createOrEmptyFolder(d)
    })

    // Then copy all binary files
    logger info "Copying chat overflow files..."

    val sourceJarDirectories = List(
      Some(s"target/scala-$scalaLibraryVersion/"),
      if (copyApi) Some(s"api/target/") else None
    ).flatten

    sourceJarDirectories.foreach(d => copyJars(d, targetDirs, logger))
  }

  /**
   * Creates a directory or empties it, by recursively deleting files and sub directories.
   */
  private def createOrEmptyFolder(path: String): Unit = {
    val folder = new File(path)
    if (folder.exists()) {
      for (file <- folder.listFiles()) {
        if (file.isFile) {
          file.delete()
        } else {
          createOrEmptyFolder(file.getAbsolutePath)
          file.delete()
        }
      }
    } else {
      folder.mkdirs()
    }
  }

  /**
   * Copies all jar files from the source to all target directories.
   */
  private def copyJars(sourceDirectory: String, targetDirectories: List[String], logger: ManagedLogger): Unit = {
    val candidates = new File(sourceDirectory)
      .listFiles().filter(f => f.isFile && f.getName.toLowerCase.endsWith(".jar"))
    for (targetDirectory <- targetDirectories; file <- candidates) {
      Files.copy(Paths.get(file.getAbsolutePath),
        Paths.get(s"$targetDirectory/${file.getName}"))
      logger info s"Finished copying file '${file.getAbsolutePath}' to '$targetDirectory'."
    }
  }
}
