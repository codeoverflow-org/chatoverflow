package org.codeoverflow.chatoverflow.connector.actor

import java.io.PrintWriter

import akka.actor.Actor

import scala.io.Source

/**
  * The file system actor provides simple utility methods to read and write files.
  */
class FileSystemActor extends Actor {

  // TODO: Subject of change?
  private val resourceFilePath = "src/main/resources/"

  /**
    * Receives either a string, or a 2-tuple of strings. Loads or saves a file.
    *
    * @return message: string => the files content, the message is interpreted as relative path
    *         message: (path, content) => true, if the file with the content of the second string could be saved
    */
  override def receive: Receive = {
    case pathInResources: String =>
      try {
        sender ! Some(Source.fromFile(s"$resourceFilePath${fixPath(pathInResources)}").mkString)
      } catch {
        case _: Exception => None
      }
    case (pathInResources: String, content: String) =>
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
    path.replace("../", "").replace("..\\", "")
  }
}