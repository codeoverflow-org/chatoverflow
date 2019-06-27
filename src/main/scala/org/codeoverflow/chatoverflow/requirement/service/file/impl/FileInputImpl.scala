package org.codeoverflow.chatoverflow.requirement.service.file.impl

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.Optional

import javax.imageio.ImageIO
import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.input.FileInput
import org.codeoverflow.chatoverflow.registry.Impl
import org.codeoverflow.chatoverflow.requirement.InputImpl
import org.codeoverflow.chatoverflow.requirement.service.file.FileConnector

@Impl(impl = classOf[FileInput], connector = classOf[FileConnector])
class FileInputImpl extends InputImpl[FileConnector] with FileInput with WithLogger {

  override def getFile(pathInResources: String): Optional[String] = Optional.ofNullable(sourceConnector.get.getFile(pathInResources).orNull)

  override def getBinaryFile(pathInResources: String): Optional[Array[Byte]] = Optional.ofNullable(sourceConnector.get.getBinaryFile(pathInResources).orNull)

  override def getImage(pathInResources: String): Optional[BufferedImage] = {
    val data = sourceConnector.get.getBinaryFile(pathInResources)
    if (data.isEmpty) {
      None
    }
    val bis = new ByteArrayInputStream(data.get)
    Optional.of(ImageIO.read(bis))
  }

  override def start(): Boolean = true

  /**
    * Stops the input, called before source connector will shutdown
    *
    * @return true if stopping was successful
    */
  override def stop(): Boolean = true
}
