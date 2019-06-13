import org.jline.terminal.TerminalBuilder

/**
  * Progress bar used reporting the status while checking and downloading libs.
  *
  * @param max count of events e.g. length of the list which progress is monitored.
  */
class ProgressBar(max: Int) {
  // Width of the terminal, used for size calculations
  private val width = {
    val width = TerminalBuilder.builder().dumb(true).build().getWidth

    // Size couldn't be figured out, use a default
    if (width <= 10)
      80
    else
      width
  }

  private var count = 0
  private var description = ""

  // We need to create a empty line so that latest line before creation won't be overwritten by the draw method.
  println()
  draw() // Initial draw

  /**
    * Increases count by 1 and re-draws the progress bar with the updated count.
    */
  def countUp(): Unit = {
    // Thread-safeness when working with parallel collections
    count.synchronized {
      count += 1
      draw()
    }
  }

  /**
    * Updates the description and re-draws the description line.
    * @param desc the new description
    */
  def updateDescription(desc: String): Unit = {
    // Thread-safeness when working with parallel collections
    description.synchronized {
      description = desc
      drawDescription()
    }
  }

  /**
    * Deletes the description to result in a blank line. The progress bar will still be visible.
    * After this you can normally print at the beginning of a new line and don't start at the end of the description.
    */
  def finish(): Unit = {
    description = ""
    drawDescription()
  }

  /**
    * Draws the progress bar in the line above the current one.
    */
  private def draw(): Unit = {
    val barWidth = width - 16 // Width of the bar without percentage and counts
    val percentage = count * 100 / max
    val equalsSigns = "=" * (barWidth * percentage / 100)
    val whiteSpaces = " " * (barWidth - equalsSigns.length)

    val content = "%3d%% (%2d|%2d) [%s]".format(percentage, count, max, equalsSigns + ">" + whiteSpaces)

    print(s"\033[1A\r$content\n")
    //          |   |    |    |
    // Go up 1 line |    |    |
    //   Go to beginning of line
    //                   |    |
    //          Actual progress bar
    //                        |
    //            Go back down for description
  }

  /**
    * Draws the description which is located in the current line.
    */
  private def drawDescription(): Unit = {
    // Cap the description at the width of the terminal, otherwise a new line is created and everything would shift.
    // If the user needs to see a really long url he can just widen his terminal.
    val content = description.take(width)

    print(s"\r\033[0K$content")
    //      |     |
    // Go to beginning
    //            |
    // Clear from cursor to end of the line
  }

}
