package org.codeoverflow.chatoverflow.requirement.service.file.impl

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream

import javax.imageio.ImageIO
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.dto.ImageFormat
import org.codeoverflow.chatoverflow.api.io.input.FileInput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.InputImpl
import org.codeoverflow.chatoverflow.requirement.service.file.FileConnector

@Impl(impl = classOf[FileInput], connector = classOf[FileConnector])
class FileInputImpl extends InputImpl[FileConnector] with FileInput with WithLogger {

  override def init(): Boolean = {
    sourceConnector.get.init()
  }

  override def getFile(pathInResources: String): String = sourceConnector.get.getFile(pathInResources).get

  override def getBinaryFile(pathInResources: String): Array[Byte] = sourceConnector.get.getBinaryFile(pathInResources).get

  override def getImage(pathInResources: String, format: ImageFormat): BufferedImage = {
    val data = sourceConnector.get.getBinaryFile(s"$pathInResources.${format.toString.toLowerCase}").get
    val bis = new ByteArrayInputStream(data)
    ImageIO.read(bis)
  }

  override def start(): Boolean = true
}
