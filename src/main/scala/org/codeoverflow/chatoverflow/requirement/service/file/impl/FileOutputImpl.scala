package org.codeoverflow.chatoverflow.requirement.service.file.impl

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream

import javax.imageio.ImageIO
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.output.FileOutput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.OutputImpl
import org.codeoverflow.chatoverflow.requirement.service.file.FileConnector

@Impl(impl = classOf[FileOutput], connector = classOf[FileConnector])
class FileOutputImpl extends OutputImpl[FileConnector] with FileOutput with WithLogger {

  override def saveFile(content: String, pathInResources: String): Boolean = {
    sourceConnector.get.saveFile(pathInResources, content)
  }

  override def saveBinaryFile(bytes: Array[Byte], pathInResources: String): Boolean = {
    sourceConnector.get.saveBinaryFile(pathInResources, bytes)
  }

  override def saveImage(image: BufferedImage, format: String, pathInResources: String): Boolean = {
    val bao = new ByteArrayOutputStream()
    ImageIO.write(image, format.toLowerCase, bao)
    sourceConnector.get.saveBinaryFile(s"$pathInResources.${format.toLowerCase}", bao.toByteArray)
  }

  override def createDirectory(folderName: String): Boolean = {
    sourceConnector.get.createDirectory(folderName)
  }

  override def exists(pathInResources: String): Boolean = {
    sourceConnector.get.exists(pathInResources)
  }

  override def delete(pathInResources: String): Boolean = {
    sourceConnector.get.delete(pathInResources)
  }

  override def start() = true

  /**
    * Stops the output, called before source connector will shutdown
    *
    * @return true if stopping was successful
    */
  override def stop(): Boolean = true
}
