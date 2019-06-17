package org.codeoverflow.chatoverflow.connector.actor

import java.io.{File, PrintWriter}
import java.nio.file.Files

import akka.actor.Actor
import org.codeoverflow.chatoverflow.Launcher
import org.codeoverflow.chatoverflow.connector.actor.FileSystemActor._

import scala.annotation.tailrec
import scala.io.Source

/**
  * The file system actor provides simple utility methods to read and write files.
  */
class FileSystemActor extends Actor {

  private val dataFilePath = Launcher.pluginDataPath

  // Create data folder if non existent
  private val dataFolder = new File(dataFilePath)
  if (!dataFolder.exists()) {
    dataFolder.mkdir()
  }

  /**
    * Receives either LoadFile or SaveFile object, acts accordingly.
    *
    * @return a loaded file or a boolean if the saving process was successful
    */
  override def receive: Receive = {
    case LoadFile(pathInResources) =>
      try {
        sender ! Some(Source.fromFile(fixPath(pathInResources)).mkString)
      } catch {
        case _: Exception => None
      }
    case LoadBinaryFile(pathInResources) =>
      try {
        sender ! Some(Files.readAllBytes(fixPath(pathInResources).toPath))
      } catch {
        case _: Exception => None
      }
    case SaveFile(pathInResources, content) =>
      try {
        val writer = new PrintWriter(fixPath(pathInResources))
        writer.write(content)
        writer.close()
        sender ! true
      } catch {
        case _: Exception => sender ! false
      }
    case SaveBinaryFile(pathInResources, content) =>
      try {
        Files.write(fixPath(pathInResources).toPath, content)
        sender ! true
      } catch {
        case _: Exception => sender ! false
      }
    case CreateDirectory(folderName) =>
      try {
        sender ! fixPath(folderName).mkdir()
      } catch {
        case _: Exception => sender ! false
      }
  }

  private def fixPath(path: String): File = {
    val fixedPath = new File(dataFolder, path).getCanonicalFile
    val dataCanonical = dataFolder.getCanonicalFile
    @tailrec def insideDataFolder(path: File): Boolean = {
      val parent = Option(path.getParentFile)
      if (parent.isEmpty) {
        false
      } else if (parent.get.equals(dataCanonical)) {
        true
      } else {
        insideDataFolder(parent.get)
      }
    }
    if (!insideDataFolder(fixedPath))
      throw new SecurityException(s"file access is restricted to resource folder (${dataFolder.getCanonicalPath})")
    fixedPath
  }
}

object FileSystemActor {

  /**
    * Send a LoadFile-object to the FileSystemActor to load a specific file and return a string.
    *
    * @param pathInResources the relative Path in the resource folder
    */
  case class LoadFile(pathInResources: String) extends ActorMessage

  /**
    * Send a LoadFile-object to the FileSystemActor to load a specific file and return a byte array.
    *
    * @param pathInResources the relative Path in the resource folder
    */
  case class LoadBinaryFile(pathInResources: String) extends ActorMessage

  /**
    * Send a SaveFile-object to the FileSystemActor to save a file with given content.
    *
    * @param pathInResources the relative Path in the resource folder
    * @param content         the content to save
    */
  case class SaveFile(pathInResources: String, content: String) extends ActorMessage

  /**
    * Send a SaveFile-object to the FileSystemActor to save a file with given content.
    *
    * @param pathInResources the relative Path in the resource folder
    * @param content         the content to save
    */
  case class SaveBinaryFile(pathInResources: String, content: Array[Byte]) extends ActorMessage

  /**
    * Send a CreateDirectory-object to the FileSystemActor to create a new sub directory.
    *
    * @param folderName the folder name. Note: Parent folder has to exist!
    */
  case class CreateDirectory(folderName: String) extends ActorMessage

}
