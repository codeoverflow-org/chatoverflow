import java.io.File
import java.nio.file.Paths

object CLI {

  /**
   * Everything in here also has to be defined in the CLI class of the framework, because
   * all arguments including bootstrap specific ones are passed through to the framework.
   * Filtering these options out would be way too difficult, because you need to know if a option
   * is a simple flag or is followed by a value. Scopt doesn't expose anything to get this so we would
   * need to use reflect, which is very ugly.
   * This, while not that elegant as I would like it to be, is just simple and works.
   */
  object ArgsParser extends scopt.OptionParser[Config]("ChatOverflow Updater") {
    opt[Unit]("ignore-updates")
      .action((_, c) => c.copy(ignoreUpdates = true))
      .text("Ignores searching for updates and directly start ChatOverflow")

    opt[File]("directory")
      .action((x, c) => c.copy(directory = x.getAbsolutePath))
      .text("The directory in which ChatOverflow will be executed")
      .validate(f =>
        if (!f.exists())
          Left("Directory doesn't exist")
        else if (!f.isDirectory)
          Left("Path isn't a directory")
        else
          Right()
      )

    opt[Unit]("help").action((_, c) => c.copy(help = true))

    override def errorOnUnknownArgument: Boolean = false

    override def reportWarning(msg: String): Unit = ()
  }

  case class Config(help: Boolean = false, directory: String = Paths.get("").toAbsolutePath.toString, ignoreUpdates: Boolean = false)

}
