package org.codeoverflow.chatoverflow.configuration

import java.io.File

import org.codeoverflow.chatoverflow.WithLogger

/**
  * The configuration folder provides a method to create a configuration folder.
  *
  * @param configFilePath the file path of the configuration folder
  */
class ConfigurationFolder(val configFilePath: String) extends WithLogger {

  val file = new File(configFilePath)

  /**
    * Creates the Folder
    *
    * @return true, if the folder was created correctly.
    */
  def createFolder(): Boolean = {
    logger debug s"Config Folder $configFilePath not found. Creating Folder."
    try {
      file.mkdir()
      logger debug s"Created Folder $configFilePath."
      true
    } catch {
      case e: Exception =>
        logger warn s"Unable to create Folder. An error occurred: ${e.getMessage}"
        false
    }
  }

  /**
    * Checks if the folder already exists
    *
    * @return true, if the folder already exists.
    */
  def doesExists(): Boolean = {
    file.exists()
  }

}
