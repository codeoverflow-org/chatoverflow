package org.codeoverflow.chatoverflow.build

import java.io.File

import sbt.internal.util.ManagedLogger

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
}
