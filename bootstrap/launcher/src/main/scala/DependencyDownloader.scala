import java.io.{File, InputStream}
import java.net.{URL, URLClassLoader}

import coursier.Fetch
import coursier.cache.FileCache
import coursier.cache.loggers.{FileTypeRefreshDisplay, RefreshLogger}
import coursier.core.{Configuration, Dependency}
import coursier.maven.{MavenRepository, PomParser}

import scala.io.Source

object DependencyDownloader {
  private val pomFile = "dependencies.pom"
  private val logger = RefreshLogger.create(System.out, FileTypeRefreshDisplay.create())
  private val cache = FileCache().noCredentials.withLogger(logger)

  // Classloader containing all jars, used to get the dependencies from the framework jar
  private val jarFiles = {
    val jarsOpt = Option(new File("bin").listFiles())
    jarsOpt.getOrElse(Array()).filter(_.getName.endsWith(".jar")).map(_.toURI.toURL)
  }
  private val classloader = new URLClassLoader(jarFiles)

  private def getPomIs: InputStream = classloader.getResourceAsStream(pomFile)

  /**
   * Parses the pom file of the framework jar and returns a seq of all dependencies that are required to run it.
   *
   * @return the seq of dependencies, if it is empty an error has occurred and logged.
   */
  private def parsePom(): Seq[Dependency] = {
    if (getPomIs == null) {
      println("Couldn't find the pom containing all required dependencies for the framework in the jar.")
      return Seq()
    }

    val pomFile = Source.fromInputStream(getPomIs)
    val parser = coursier.core.compatibility.xmlParseSax(pomFile.mkString, new PomParser)
    parser.project match {
      case Right(deps) =>
        deps.dependencies
          .filterNot(_._1 == Configuration.provided) // Provided deps are... well provided and no download is required
          .map(_._2)
          .filter(_.module.name.value != "chatoverflow-api") // We already have the api locally inside the bin directory
      case Left(errorMsg) =>
        println(s"Pom containing all required dependencies for the framework couldn't be parsed: $errorMsg")
        Seq()
    }
  }

  /**
   * Fetches all required dependencies for the framework using Coursier.
   *
   * @return a seq of urls of jar files that need to be included to the classpath. A empty seq signifies an error.
   */
  def fetchDependencies(): Seq[URL] = {
    val deps = parsePom()
    if (deps.isEmpty)
      return Seq()

    // IntelliJ may warn you that a implicit is missing. This is one of the many bugs in IntelliJ, the code compiles fine.
    val jars: Seq[File] = Fetch()
      .withCache(cache)
      .addDependencies(deps: _*)
      .addRepositories(MavenRepository("https://jcenter.bintray.com")) // JCenter is used for JDA (DiscordConnector)
      .run

    jars.map(_.toURI.toURL)
  }
}
