package org.codeoverflow.chatoverflow.instance

import org.codeoverflow.chatoverflow.WithLogger
import org.codeoverflow.chatoverflow.api.io.input.Input
import org.codeoverflow.chatoverflow.api.io.output.Output
import org.codeoverflow.chatoverflow.api.plugin.configuration.{Requirement, Requirements}
import org.codeoverflow.chatoverflow.api.plugin.{Plugin, PluginManager}
import org.codeoverflow.chatoverflow.framework.PluginCompatibilityState.PluginCompatibilityState
import org.codeoverflow.chatoverflow.framework.manager.{PluginManagerImpl, PluginManagerStub}
import org.codeoverflow.chatoverflow.framework.{PluginCompatibilityState, PluginType}

/**
  * A plugin instance holds all the general information of the plugin type and specific information of
  * the plugin execution and state.
  *
  * @param instanceName the unique name of the plugin instance
  * @param pluginType   the type of the plugin that should be created
  */
class PluginInstance(val instanceName: String, pluginType: PluginType) extends WithLogger {
  private var plugin: Option[Plugin] = None
  private var instanceThread: Thread = _
  private var threadStopAfterNextIteration = false

  /**
    * Returns the name of the plugin type.
    *
    * @return the display name of the plugin type
    */
  def getPluginTypeName: String = pluginType.getName

  /**
    * Returns the author name of the plugin type.
    *
    * @return the real name or a alias of the author
    */
  def getPluginTypeAuthor: String = pluginType.getAuthor

  /**
    * Returns a description of the plugin type.
    *
    * @return a simple description of the service
    */
  def getPluginTypeDescription: String = pluginType.getDescription

  /**
    * Returns the newest version of the api, where the plugin type was successfully tested!
    *
    * @return a version number in format "major.minor", e.g. "1.0"
    */
  def getPluginTypeVersion: String = s"${pluginType.getMajorAPIVersion}.${pluginType.getMinorAPIVersion}"

  /**
    * Returns the state of of the plugin (if its API Version is compatible).
    * If there is no state known, the API state is tested first.
    *
    * @return a plugin version state object filled with the current status information
    */
  def getPluginTypeCompatibilityState: PluginCompatibilityState = {
    if (pluginType.getState == PluginCompatibilityState.Untested) {
      pluginType.testState
    } else {
      pluginType.getState
    }
  }

  /**
    * Returns the plugin manager, previously created and set by the framework.
    *
    * @return a instance specific plugin manager object
    */
  def getPluginManager: PluginManager = if (isCreated) plugin.get.getManager else {
    logger error s"Plugin instance '$instanceName' does not exist."
    new PluginManagerStub()
  }

  /**
    * Returns, if the plugin instance is created. This should be always the case when a plugin instance
    * is added to the registry.
    */
  private def isCreated: Boolean = plugin.isDefined

  /**
    * Creates a new thread and tries to start the plugin.
    * Make sure to set the requirements first!
    *
    * @return true if the plugin execution thread could be created and the plugin is started
    *         false if the plugin is already running, is no created, not all needed requirements are set
    *         or the plugin starting process fails
    */
  def start(): Boolean = {
    threadStopAfterNextIteration = false

    // First, check if already running (cannot start a plugin instance twice, obviously)
    if (isRunning) {
      logger warn s"Unable to start plugin instance '$instanceName'. Already running!"
      false
    } else {

      // Check if created first
      if (!isCreated) {
        // This should never happen
        logger error s"Unable to start plugin instance '$instanceName'. Call createPluginInstance() first."
        false
      } else {

        // Next, check for complete requirements
        if (!getRequirements.isComplete) {
          logger error s"At least one non-optional requirement of plugin '$instanceName' has not been set. Unable to start!"
          logger debug s"Not set requirements: ${getRequirements.getMissingRequirements.toArray.mkString(", ")}."
          false

        } else {

          // This is set to false if any connector (aka input/output) is not ready.
          var allConnectorsReady = true

          // Initialize all inputs & outputs
          val inputRequirements = getRequirements.getInputRequirements.toArray
          for (requirement <- inputRequirements) {
            try {
              val input = requirement.asInstanceOf[Requirement[Input]]
              if (input.isSet) {
                if (!input.get().init()) {
                  logger warn s"Failed to init connector (input) '${input.getName}' of type '${input.getTargetType.getName}'."
                  allConnectorsReady = false
                }
              }
            } catch {
              case e: Exception =>
                logger warn s"Unable to initialize input '$requirement'. Exception: ${e.getMessage}"
            }
          }
          val outputRequirements = getRequirements.getOutputRequirements.toArray
          for (requirement <- outputRequirements) {
            try {
              val output = requirement.asInstanceOf[Requirement[Output]]
              if (output.isSet) {
                if (!output.get().init()) {
                  logger warn s"Failed to init connector (output) '${output.getName}' of type '${output.getTargetType.getName}'."
                  allConnectorsReady = false
                }
              }
            } catch {
              case e: Exception =>
                logger warn s"Unable to initialize output '$requirement'. Exception: ${e.getMessage}"
            }
          }

          if (!allConnectorsReady) {
            logger error "At least one connector (input/output) did fail init. Unable to start."
            false
          } else {

            // Now, start the plugin!
            logger info s"Starting plugin '$instanceName' in new thread!"
            try {
              instanceThread = new Thread(() => {
                try {

                  // Execute plugin setup
                  plugin.get.setup()

                  // Execute loop, if an interval is set
                  if (plugin.get.getLoopInterval > 0) {
                    while (!threadStopAfterNextIteration) {
                      val startTime = System.currentTimeMillis()

                      plugin.get.loop()

                      val execTime = System.currentTimeMillis() - startTime
                      val sleepTime = plugin.get.getLoopInterval - execTime
                      if (sleepTime > 0)
                        Thread.sleep(sleepTime)
                    }
                  }

                  // After the loop (or setup) the plugin should end
                  plugin.get.shutdown()

                  logger info s"Stopped plugin instance '$instanceName'."

                } catch {
                  case e: AbstractMethodError => logger.error(s"Plugin '$instanceName' just crashed. Looks like a plugin version error.", e)
                  case e: Exception => logger.error(s"Plugin '$instanceName' just had an exception. Might be a plugin implementation fault.", e)
                  case e: Throwable => logger.error(s"Plugin '$instanceName' just crashed. We don't know whats going up here!", e)
                }
              })
              instanceThread.start()
              true
            } catch {
              case e: Throwable =>
                logger.error(s"Plugin starting process of plugin '$instanceName' just crashed.", e)
                false
            }
          }
        }
      }
    }
  }

  /**
    * Returns the requirements object of the specific plugin instance.
    *
    * @return the requirements object of the plugin. If the plugin has not been instantiated, it is empty
    */
  def getRequirements: Requirements = {
    if (isCreated) {
      plugin.get.getRequirements
    } else {
      // This should never happen
      logger error s"Plugin instance '$instanceName' does not exist."
      new Requirements()
    }
  }

  /**
    * Returns if the plugin is currently executed (the thread is running)
    *
    * @return true, if the plugin execution thread is defined and alive
    */
  def isRunning: Boolean = {
    if (instanceThread == null) {
      false
    } else {
      instanceThread.isAlive
    }
  }

  /**
    * Tells the plugin to stop its execution after the next iteration of the loop()-method and its sleeping-cycle.
    */
  def stopPlease(): Unit = {
    logger info s"Requested stop of plugin instance '$instanceName'."
    threadStopAfterNextIteration = true
  }

  /**
    * Creates a new plugin instance with the default manager implementation.
    * Note: This is instance-private, because non-instantiatable plugins should be not added in the registry
    */
  private[instance] def createPluginInstanceWithDefaultManager(logOutputOnConsole: Boolean): Boolean = {
    createPluginInstance(new PluginManagerImpl(instanceName, logOutputOnConsole))
  }

  /**
    * Creates a new plugin instance with the given plugin manager.
    * Note: This is instance-private, because non-instantiatable plugins should be not added in the registry
    */
  private[instance] def createPluginInstance(manager: PluginManager): Boolean = {
    plugin = pluginType.createPluginInstance(manager)

    // Return if instantiation was successful
    if (plugin.isEmpty) {
      false
    } else {
      true
    }
  }
}
