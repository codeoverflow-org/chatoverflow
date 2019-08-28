package org.codeoverflow.chatoverflow.framework.helper

import java.io.{File, InputStream}

import coursier.Fetch
import coursier.cache.{CacheLogger, FileCache}
import coursier.core.{Configuration, Dependency}
import coursier.maven.PomParser
import org.codeoverflow.chatoverflow.WithLogger

import scala.io.Source

/**
 * A utility object containing some common code for use with Coursier.
 */
object CoursierUtils extends WithLogger {

  private object CoursierLogger extends CacheLogger {
    override def downloadedArtifact(url: String, success: Boolean): Unit = {
      logger debug (if (success)
        s"Successfully downloaded $url"
      else
        s"Failed to download $url")
    }
  }

  private val cache = FileCache().noCredentials.withLogger(CoursierLogger)

  /**
   * Extracts all dependencies out of the provided pom. Throws an exception if the pom is invalid.
   *
   * @param is the InputStream from which the pom is read
   * @return a seq of all found dependencies
   */
  def parsePom(is: InputStream): Seq[Dependency] = {
    val pomFile = Source.fromInputStream(is)
    val parser = coursier.core.compatibility.xmlParseSax(pomFile.mkString, new PomParser)

    parser.project match {
      case Right(deps) => deps.dependencies
        .filterNot(_._1 == Configuration.provided) // Provided deps are... well provided and no download is required
        .map(_._2)
      case Left(errorMsg) => throw new IllegalArgumentException(s"Pom couldn't be parsed: $errorMsg")
    }
  }

  /**
   * Resolves and fetches all passed dependencies and gives back a seq of all local files of these dependencies.
   *
   * @param dependencies all dependencies that you want to be fetched
   * @return all local files for the passed dependencies
   */
  def fetchDependencies(dependencies: Seq[Dependency]): Seq[File] = {
    // IntelliJ may warn you that a implicit is missing. This is one of the many bugs in IntelliJ, the code compiles fine.
    Fetch()
      .withCache(cache)
      .addDependencies(dependencies: _*)
      .run()
  }
}
