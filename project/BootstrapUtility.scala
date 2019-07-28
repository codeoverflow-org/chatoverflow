import java.io.File
import java.nio.file.{Files, Paths}

import BuildUtility.withTaskInfo
import sbt.internal.util.ManagedLogger
import sbt.librarymanagement.ModuleID

import scala.xml.XML

/**
  * Holds the functionality to read all dependencies and feed the bootstrap launcher with this information.
  * Should be used once for every new public version of chat overflow.
  */
object BootstrapUtility {
  val dependencyProjectBasePath = "bootstrap/src/main"
  val dependencyXMLFileName = s"$dependencyProjectBasePath/resources/dependencies.xml"

  /**
    * This task retrieves all dependencies and creates a xml file for the bootstrap launcher.
    *
    * @param logger              the sbt logger
    * @param scalaLibraryVersion the current scala library version
    */
  def bootstrapGenTask(logger: ManagedLogger, scalaLibraryVersion: String, modules: List[ModuleID]): Unit = {
    withTaskInfo("BOOTSTRAP GENERATION", logger) {

      // Dependency management
      val dependencyList = retrieveDependencies(logger, scalaLibraryVersion, modules)
      saveDependencyXML(dependencyList, logger)
    }
  }

  /**
    * Saves all dependencies as XML in the bootstrap launcher dependency xml file.
    */
  private def saveDependencyXML(dependencyList: List[Dependency], logger: ManagedLogger): Unit = {

    logger info "Started saving dependency XML."

    val xml =
      <dependencies>
        {for (dependency <- dependencyList) yield dependency.toXML}
      </dependencies>

    logger info "Saving dependency XML now."

    // Create directory if not existent, otherwise saving will fail.
    if (new File(dependencyXMLFileName).getParentFile.mkdir()) {
      logger info "Created directory bootstrap/src/main/resources"
    }

    XML.save(dependencyXMLFileName, xml)
    logger info "Finished saving XML file."
  }

  /**
    * Converts passed modules to a list of Dependency, resolves them to urls, filters out codeoverflow
    * and adds the scala library to the list.
    */
  private def retrieveDependencies(logger: ManagedLogger, scalaLibraryVersion: String, modules: List[ModuleID]): List[Dependency] = {

    logger info "Starting dependency retrieval."

    logger info "Creating dependency list and resolve dependencies."

    val dependencyList = for (dep <- modules) yield {
      val dependencyString = s"${dep.organization}:${dep.name}:${dep.revision}"
      new Dependency(dependencyString, logger)
    }

    logger info "Updating and modifying dependencies..."

    // Modify dependencies: Remove ChatOverflow and opus-java, add scala library
    // opus-java is a virtual package which instructs sbt or any other build tool to get opus-java-api and opus-java-native.
    // Therefore it doesn't have a jar that needs to be downloaded and sbt includes the requested dependencies in the dependencyList.
    // So we can just ignore it as it can't be resolved and only need to include the requested deps in our xml.
    // Check https://github.com/discord-java/opus-java#opus-java-1 for more information on this.
    val excludedDeps = List("chatoverflow", "chatoverflow-api", "opus-java")
    val filteredDeps = dependencyList.filter(d => !excludedDeps.contains(d.nameWithoutScalaVersion))

    val modifiedDependencies = filteredDeps ++
      List(new Dependency(s"org.scala-lang:scala-library:$scalaLibraryVersion", logger))

    // Info output
    logger info s"Found ${modifiedDependencies.length} dependencies."
    if (modifiedDependencies.exists(d => !d.available)) {
      logger warn "Found the following dependencies, that could not be retrieved online:"
      logger warn modifiedDependencies.filter(d => !d.available).map(_.toString).mkString("\n")
    }

    modifiedDependencies
  }

  /**
   * Prepares the environment for deployment. Should be called after package and assembly task.
   *
   * @param logger              the sbt logger
   * @param scalaLibraryVersion the scala library major version
   */
  def prepareDeploymentTask(logger: ManagedLogger, scalaLibraryVersion: String): Unit = {
    // Assuming, before this: clean, bs, assembly bootstrapProject, package
    // Assuming: Hardcoded "bin/" and "deploy/" folders
    // Assuming: A folder called "deployment-files" with all additional files (license, bat, etc.)

    withTaskInfo("PREPARE DEPLOYMENT", logger) {

      logger info "Started deployment process."

      // First step: Create directories
      createOrEmptyFolder("deployDev")

      // Second step: Create bin directories and copy all binaries
      val targetJarDirectories = List("bin", "deploy/bin")
      prepareBinDirectories(logger, targetJarDirectories, scalaLibraryVersion)

      // Third step: Copy bootstrap launcher
      copyJars(s"bootstrap/target/scala-$scalaLibraryVersion/", List("deploy/"), logger)

      // Last step: Copy additional files
      logger info "Copying additional deployment files..."
      val deploymentFiles = new File("deployment-files/")
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
   */
  def prepareDevDeploymentTask(logger: ManagedLogger, scalaLibraryVersion: String): Unit = {
    // Assuming, before this: clean, package and apiProject/packagedArtifacts
    // Assuming: Hardcoded "bin/" and "deployDev/" folders
    // Assuming: A folder called "deployment-files-dev" with more additional files for plugin developers

    withTaskInfo("PREPARE DEV DEPLOYMENT", logger) {

      logger info "Started deployment process for plugin dev environment."

      // First step: Create directories
      createOrEmptyFolder("deployDev")

      // Second step: Copy all binaries
      val targetJarDirectories = List("bin", "deployDev/bin")
      prepareBinDirectories(logger, targetJarDirectories, scalaLibraryVersion)

      // Third step: Copy required meta-build files
      val requiredBuildFiles = Set("BuildUtility.scala", "build.properties", "Plugin.scala", "PluginCreateWizard.scala",
        "PluginLanguage.scala", "PluginMetadata.scala", "SbtFile.scala")

      for (filepath <- requiredBuildFiles) {
        val origFile = new File(s"project/$filepath")
        val deployFile = new File(s"deployDev/project/$filepath")
        sbt.IO.copyFile(origFile, deployFile)
      }

      // Last step: Copy additional files
      val devDeploymentFiles = new File("deployment-files-dev/")
      if (!devDeploymentFiles.exists()) {
        logger warn "Unable to find dev deployment files."
      } else {
        sbt.IO.copyDirectory(devDeploymentFiles, new File("deployDev/"))
        logger info "Finished copying additional dev deployment files."
      }
    }
  }

  private def prepareBinDirectories(logger: ManagedLogger, targetDirs: List[String], scalaLibraryVersion: String): Unit = {
    // First prepare all bin folders
    targetDirs.foreach(d => {
      logger info s"Preparing '$d' folder."
      createOrEmptyFolder(d)
    })

    // Then copy all binary files
    logger info "Copying chat overflow files..."
    val sourceJarDirectories = List(s"target/scala-$scalaLibraryVersion/",
      s"api/target/scala-$scalaLibraryVersion/")

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