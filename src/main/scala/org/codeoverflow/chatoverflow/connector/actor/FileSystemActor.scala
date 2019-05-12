package org.codeoverflow.chatoverflow.connector.actor

import java.io.PrintWriter
import java.nio.file.Paths

import akka.actor.Actor

import scala.io.Source

/**
  * The file system actor provides simple utility methods to read and write files.
  */
class FileSystemActor extends Actor {

  // TODO: Subject of change?
  private val resourceFilePath = "src/main/resources"

  /**
    * Receives either LoadFile or SaveFile object, acts accordingly.
    *
    * @return a loaded file or a boolean if the saving process was successful
    */
  override def receive: Receive = {
    case LoadFile(pathInResources) =>
      try {
        sender ! Some(Source.fromFile(s"$resourceFilePath${fixPath(pathInResources)}").mkString)
      } catch {
        case _: Exception => None
      }
    case SaveFile(pathInResources, content) =>
      try {
        val writer = new PrintWriter(s"$resourceFilePath${fixPath(pathInResources)}")
        writer.write(content)
        writer.close()
        sender ! true
      } catch {
        case _: Exception => sender ! false
      }
  }

  private def fixPath(path: String): String = {
    val fixedPath = Paths.get("/", path).normalize()
    fixedPath.toString
  }
}

/**
  * Send a LoadFile-object to the FileSystemActor to load a specific file.
  *
  * @param pathInResources the relative Path in the resource folder
  */
case class LoadFile(pathInResources: String)

/**
  * Send a SaveFile-object to the FileSystemActor to save a file with given content.
  *
  * @param pathInResources the relative Path in the resource folder
  * @param content         the content to save
  */
case class SaveFile(pathInResources: String, content: String)