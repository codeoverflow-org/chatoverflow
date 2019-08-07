package org.codeoverflow.chatoverflow.build

import java.io.File
import java.util.jar.Manifest

import com.fasterxml.jackson.databind.ObjectMapper
import org.codeoverflow.chatoverflow.build.BuildUtils.withTaskInfo
import sbt.internal.util.ManagedLogger
import sbt.util.{FileFunction, FilesInfo}

import scala.io.Source

class GUIUtility(logger: ManagedLogger) {

  def guiTask(guiProjectPath: String, cacheDir: File): Unit = {
    withTaskInfo("BUILD GUI", logger) {
      val guiDir = new File(guiProjectPath)
      if (!guiDir.exists()) {
        logger warn s"GUI not found at $guiProjectPath, ignoring GUI build."
        return
      }

      val packageJson = new File(guiDir, "package.json")

      if (!executeNpmCommand(guiDir, cacheDir, Set(packageJson), "install",
        () => logger error "GUI dependencies couldn't be installed, please check above log for further details.",
        () => new File(guiDir, "node_modules")
      )) {
        return // early return on failure, error has already been displayed
      }

      val srcFiles = BuildUtils.getAllDirectoryChilds(new File(guiDir, "src"))
      val outDir = new File(guiDir, "dist")

      executeNpmCommand(guiDir, cacheDir, srcFiles + packageJson, "run build",
        () => logger error "GUI couldn't be built, please check above log for further details.",
        () => outDir
      )
    }
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
      val exitCode = new ProcessBuilder(getNpmCommand ++ command.split("\\s+"): _*)
        .inheritIO()
        .directory(workDir)
        .start()
        .waitFor()

      if (exitCode != 0) {
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
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      List("cmd.exe", "/C", "npm")
    } else {
      List("npm")
    }
  }

  def packageGUITask(guiProjectPath: String, scalaMajorVersion: String, crossTargetDir: File): Unit = {
    val dir = new File(guiProjectPath, "dist")
    if (!dir.exists()) {
      logger info "GUI hasn't been compiled. Won't create a jar for it."
      return
    }

    val files = BuildUtils.getAllDirectoryChilds(dir)

    // contains tuples with the actual file as the first value and the name with directory in the jar as the second value
    val jarEntries = files.map(file => file -> s"/chatoverflow-gui/${dir.toURI.relativize(file.toURI).toString}")

    val guiVersion = getGUIVersion(guiProjectPath).getOrElse("unknown")

    sbt.IO.jar(jarEntries, new File(crossTargetDir, s"chatoverflow-gui_$scalaMajorVersion-$guiVersion.jar"), new Manifest())
  }

  private def getGUIVersion(guiProjectPath: String): Option[String] = {
    val packageJson = new File(s"$guiProjectPath/package.json")
    if (!packageJson.exists()) {
      logger error "The package.json file of the GUI doesn't exist. Have you cloned the GUI in the correct directory?"
      return None
    }

    val content = Source.fromFile(packageJson)
    val version = new ObjectMapper().reader().readTree(content.mkString).get("version").asText()

    content.close()

    if (version.isEmpty) {
      logger warn "The GUI version couldn't be loaded from the package.json."
      None
    } else {
      Option(version)
    }
  }
}
