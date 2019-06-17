package org.codeoverflow.chatoverflow.requirement.service.file

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.connector.Connector
import org.codeoverflow.chatoverflow.connector.actor.FileSystemActor
import org.codeoverflow.chatoverflow.connector.actor.FileSystemActor._

class FileConnector(override val sourceIdentifier: String) extends Connector(sourceIdentifier) with WithLogger {
  override protected var requiredCredentialKeys: List[String] = List()
  override protected var optionalCredentialKeys: List[String] = List()
  private val fileActor = createActor[FileSystemActor]()

  def getFile(pathInResources: String): Option[String] = {
    val file: Option[Option[String]] = fileActor.??[Some[String]](5) {LoadFile(pathInResources)}
    if (file.isDefined) {
      file.get
    }else{
      None
    }
  }

  def getBinaryFile(pathInResources: String): Option[Array[Byte]] = {
    val binaryFile: Option[Option[Array[Byte]]] = fileActor.??[Some[Array[Byte]]](5){LoadBinaryFile(pathInResources)}
    if(binaryFile.isDefined){
      binaryFile.get
    }else{
      None
    }
  }

  def saveFile(pathInResources: String, content: String): Boolean = fileActor.??[Boolean](5){SaveFile(pathInResources, content)}.get

  def saveBinaryFile(pathInResources: String, content: Array[Byte]): Boolean = fileActor.??[Boolean](5){SaveBinaryFile(pathInResources, content)}.get

  def createDirectory(folderName: String): Boolean = fileActor.??[Boolean](5){CreateDirectory(folderName)}.get

  override def start(): Boolean = {
    logger info s"Started file connector! Source identifier is: '$sourceIdentifier'."
    true
  }

  override def stop(): Boolean = {
    logger info "Stopped file connector!"
    true
  }

}
