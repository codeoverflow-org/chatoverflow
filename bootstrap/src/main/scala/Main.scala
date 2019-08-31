import org.fusesource.jansi.internal.CLibrary

import scala.io.StdIn

object Main {

  /**
   * Software entry point
   *
   * @param args arguments for the launcher
   */
  def main(args: Array[String]): Unit = {
    println("Starting ChatOverflow Bootstrap Launcher.")

    println("Checking for updates...")
    val update = Updater.searchForUpdates
    if (update.isDefined) {
      println("A new update is available!")
      println(s"Current version: ${Updater.getCurrentVersion.get}")
      println(s"Newest version: ${update.get.tag_name}")

      if (CLibrary.isatty(CLibrary.STDIN_FILENO) == 1) {
        print(s"Do you want to download and install the update? [y/N] ")
        val in = StdIn.readLine

        if (in.toLowerCase == "y") {
          val file = Updater.downloadUpdate(update.get)
          if (file.isDefined) {
            Updater.installUpdate(file.get)
            Updater.restartBootstrapLauncher(args)
            return
          }
        }
      } else {
        println("Currently running in a non-interactive session. Please run in an interactive session to auto-update\n" +
          s"or download and install manually from ${update.get.html_url}")
      }
    } else {
      println("No new update is available.")
    }

    Bootstrap.start(args)
  }
}
