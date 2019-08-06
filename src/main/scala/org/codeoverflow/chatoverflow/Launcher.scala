package org.codeoverflow.chatoverflow

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import org.apache.log4j.{FileAppender, Level, Logger, PatternLayout}
import org.codeoverflow.chatoverflow.ui.CLI.parse
import org.codeoverflow.chatoverflow.ui.web.Server

/**
  * The launcher object is the entry point to the chat overflow framework.
  * All UI and framework related tasks are started here.
  */
object Launcher extends WithLogger {

  var server: Option[Server] = None
  var pluginDataPath: String = "data"
  private var chatOverflow: Option[ChatOverflow] = None

  /**
    * Software entry point.
    */
  def main(args: Array[String]): Unit = {
    parse(args) { config =>

      // Enable logging to log files if specified
      if (config.logFileOutput) {
        val fileName = s"log/${
          OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm-ss-SSS"))
        }.log"

        // Try to retrieve the console appender layout or create a default one
        val pattern = if (logger.getAppender("CA") != null) {
          logger.getAppender("CA").getLayout
        } else new PatternLayout("%-4r %d{HH:mm:ss.SSS} [%t] %-5p %c{2} %x - %m%n")

        // This is the pattern from our basic console logger
        val appender = new FileAppender(pattern, fileName, true)
        appender.setThreshold(Level.ALL)

        Logger.getRootLogger.addAppender(appender)
      }

      // Set globally available plugin data path
      this.pluginDataPath = config.pluginDataPath

      // The complete project visible trough one single instance
      val chatOverflow = new ChatOverflow(config.pluginFolderPath,
        config.configFolderPath, config.requirementPackage, config.pluginLogOutputOnConsole)
      this.chatOverflow = Some(chatOverflow)

      // Initialize chat overflow
      chatOverflow.init()

      // Login if a password is specified
      if (config.loginPassword.nonEmpty && !chatOverflow.isLoaded) {
        chatOverflow.credentialsService.setPassword(config.loginPassword)
        chatOverflow.load()
      }

      // Launch server (UI backend)
      startServer(chatOverflow, config.webServerPort)

      // Start plugins if specified
      if (config.startupPlugins.nonEmpty) {
        if (chatOverflow.isLoaded) {
          logger info "Starting startup plugins."
          for (instanceName <- config.startupPlugins) {
            val instance = chatOverflow.pluginInstanceRegistry.getPluginInstance(instanceName)

            if (instance.isEmpty) {
              logger warn s"No plugin instance named '$instanceName' was registered."
            } else {
              instance.get.start()
            }
          }
        } else {
          logger warn "Unable to run startup plugins. No/wrong password supplied."
        }
      }
    }
  }

  private def startServer(chatOverflow: ChatOverflow, port: Int): Unit = {
    if (server.isEmpty) {
      server = Some(new Server(chatOverflow, port))
      server.get.startAsync()
    }
  }

  /**
    * Shuts down the framework.
    */
  def exit(): Unit = {
    logger debug "Shutting down Chat Overflow."

    if (server.isDefined) {
      logger info "Stopping the server."
      server.get.stop()
    }

    if (chatOverflow.isDefined) {
      logger info "Saving all settings."
      chatOverflow.get.save()

      logger debug "Trying to stop all running instances..."
      chatOverflow.get.pluginInstanceRegistry.getAllPluginInstances.
        filter(_.isRunning).foreach(_.stopPlease())

      for (i <- 1 to 5) {
        if (chatOverflow.get.pluginInstanceRegistry.getAllPluginInstances.
          exists(_.isRunning)) {
          logger info s"Check $i of 5. Still waiting on: ${
            chatOverflow.get.pluginInstanceRegistry.getAllPluginInstances.
              filter(_.isRunning).map(_.instanceName).mkString(", ")
          }"
          Thread.sleep(1000)
        }
      }
    }

    logger info "Bye Bye. Stay minzig!"
    System.exit(0)
  }
}
