package org.codeoverflow.chatoverflow.build

import java.io.File

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import sbt.internal.util.ManagedLogger
import sbt.util.{FileFunction, FilesInfo}

import scala.io.Source
import scala.util.Try

class GUIUtility(logger: ManagedLogger) {

  def guiTask(guiProjectPath: String, cacheDir: File, classesDir: File): Unit = {
    val guiDir = new File(guiProjectPath)
    if (!guiDir.exists()) {
      logger warn s"GUI not found at $guiProjectPath, ignoring GUI build."
      return
    }

    val packageJson = new File(guiDir, "package.json")

    logger info "Installing NPM dependencies of the gui ..."
    if (!executeNpmCommand(guiDir, cacheDir, Set(packageJson), "install",
      () => logger error "GUI dependencies couldn't be installed, please check above log for further details.",
      () => {
        logger info "Successfully installed NPM dependencies of the gui."
        new File(guiDir, "node_modules")
      }
    )) {
      return // early return on failure, error has already been displayed
    }

    val srcFiles = BuildUtils.getAllDirectoryChilds(new File(guiDir, "src"))
    val outDir = new File(guiDir, "dist")

    logger info "Compiling Angular GUI ..."
    executeNpmCommand(guiDir, cacheDir, srcFiles + packageJson, "run build",
      () => logger error "GUI couldn't be built, please check above log for further details.",
      () => {
        logger info "Successfully compiled Angular GUI."
        outDir
      }
    )

    sbt.IO.copyDirectory(outDir, new File(classesDir, "chatoverflow-gui"))
    writeVersionFiles(new File(guiProjectPath), classesDir)
  }

  /**
   * Executes a npm command in the given directory and skips executing the given command
   * if no input files have changed and the output file still exists.
   *
   * @param workDir  the directory in which npm should be executed
   * @param cacheDir a directory required for caching using sbt
   * @param inputs   the input files, which will be used for caching.
   *                 If any one of these files change the cache is invalidated.
   * @param command  the npm command to execute
   * @param failed   called if npm returned an non-zero exit code
   * @param success  called if npm returned successfully. Needs to return a file for caching.
   *                 If the returned file doesn't exist the npm command will ignore the cache.
   * @return true if npm returned zero as a exit code and false otherwise
   */
  private def executeNpmCommand(workDir: File, cacheDir: File, inputs: Set[File], command: String,
                                failed: () => Unit, success: () => File): Boolean = {
    // sbt allows easily to cache our external build using FileFunction.cached
    // sbt will only invoke the passed function when at least one of the input files (passed in the last line of this method)
    // has been modified. For the gui these input files are all files in the src directory of the gui and the package.json.
    // sbt passes these input files to the passed function, but they aren't used, we just instruct npm to build the gui.
    // sbt invalidates the cache as well if any of the output files (returned by the passed function) doesn't exist anymore.
    val cachedFn = FileFunction.cached(new File(cacheDir, command), FilesInfo.hash) { _ => {
      val process = new ProcessBuilder(getNpmCommand ++ command.split("\\s+"): _*)
        .directory(workDir)
        .start()

      val exitCode = process.waitFor()
      if (exitCode != 0) {
        // Stream error message to the user
        Source.fromInputStream(process.getErrorStream).getLines().foreach(println)
        failed()
        return false
      } else {
        Set(success())
      }
    }
    }

    cachedFn(inputs)
    true
  }

  private def getNpmCommand: List[String] = {
    if (BuildUtils.isRunningOnWindows) {
      List("cmd.exe", "/C", "npm")
    } else {
      List("npm")
    }
  }

  def getPackageJson(guiProjectPath: File): Option[JsonNode] = Try {
    val packageJson = new File(guiProjectPath, "package.json")
    if (!packageJson.exists()) {
      logger error "The package.json file of the GUI doesn't exist. Have you cloned the GUI in the correct directory?"
      return None
    }

    val content = Source.fromFile(packageJson)
    val json = new ObjectMapper().reader().readTree(content.mkString)

    content.close()

    Some(json)
  }.getOrElse(None)

  def getGUIVersion(packageJson: JsonNode): Option[String] = {
    if (packageJson.has("version")) {
      val version = packageJson.get("version").asText()

      if (version.isEmpty) {
        logger warn "The GUI version couldn't be loaded from the package.json."
        None
      } else {
        Option(version)
      }
    } else {
      None
    }
  }

  def getRestVersion(packageJson: JsonNode): Option[String] = {
    if (packageJson.has("dependencies") && packageJson.get("dependencies").hasNonNull("@codeoverflow-org/chatoverflow")) {
      val version = packageJson.get("dependencies").get("@codeoverflow-org/chatoverflow").asText()

      if (version.isEmpty) {
        logger warn "The used REST api version couldn't be loaded from the package.json."
        None
      } else {
        Option(version)
      }
    } else {
      None
    }
  }

  private def writeVersionFiles(guiProjectDir: File, classDir: File): Unit = {
    val json = getPackageJson(guiProjectDir)
    if (json.isDefined) {
      getGUIVersion(json.get).foreach { ver =>
        val f = new File(classDir, "version_gui.txt")
        sbt.IO.write(f, ver)
      }
      getRestVersion(json.get).foreach { ver =>
        val f = new File(classDir, "version_gui_rest.txt")
        sbt.IO.write(f, ver)
      }
    }
  }
}
