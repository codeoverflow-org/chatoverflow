import sbt.internal.util.ManagedLogger

class Dependency(dependencyString: String, logger: ManagedLogger) {
  var nameWithoutScalaVersion = ""
  var version = ""
  var url = ""
  var available = false
  create()

  private def create(): Unit = {
    val DependencyRegex = "([^:]+):([^:_]+)(_[^:]+)?:([^:]+)".r
    // TODO: HTTPS?
    val mavenCentralFormat = "http://central.maven.org/maven2/%s/%s/%s/%s.jar"

    dependencyString match {
      case DependencyRegex(depAuthor, depName, scalaVersion, depVersion) =>
        this.nameWithoutScalaVersion = depName
        this.version = depVersion

        val combinedName = if (scalaVersion != null) depName + scalaVersion else depName

        // Create URL for maven central
        url = mavenCentralFormat.format(depAuthor.replaceAll("\\.", "/"), s"$combinedName",
          depVersion, s"$combinedName-$depVersion")

        logger info url
      // TODO: Test availability of URL without full download (?), save into available
      case _ =>
        logger warn s"Invalid dependency format: '$dependencyString'."
    }
  }

}

object BootstrapUtility {

  def bootstrapDependencyGenTask(logger: ManagedLogger): Unit = {
    println("Welcome to the bootstrap generation utility. It's time to build!")
    println("Please enter the console output of the task 'dependencyList' without the intro part (so only the dependencies).")
    println("Should look like '[info] ... [info] ...'")
    println("> ")

    // We just assume that the building guy knows what he's doing, lol
    val input = scala.io.Source.fromInputStream(System.in).bufferedReader().readLine()

    // Splits the lines at the info tag of the console output
    val lines = input.replaceFirst("\\[info\\] ", "").split(" \\[info\\] ")
    val dependencies = for (line <- lines) yield new Dependency(line, logger)

    //val testString = "[info] log4j:log4j:1.2.17"
    //logger info testString
    //logger info removeInfo(testString)
    //new Dependency(removeInfo(testString), logger)
  }

  private def removeInfo(dependencyString: String): String = {
    val InfoRegex = "^\\[info\\] ".r
    InfoRegex.replaceFirstIn(dependencyString, "")
  }

}