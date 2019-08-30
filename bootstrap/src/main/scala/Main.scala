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
      println(s"Go download it at ${update.get.html_url}")
    } else {
      println("No new update is available.")
    }

    Bootstrap.start(args)
  }
}
