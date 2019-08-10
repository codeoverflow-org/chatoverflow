package org.codeoverflow.chatoverflow.connector

import org.codeoverflow.chatoverflow.requirement.impl.EventManager

/**
 * A connector is used to connect a input / output service to its dedicated platform.
 * Also has methods to register and fire events.
 *
 * @param sourceIdentifier the unique source identifier (e.g. a login name), the connector should work with
 */
abstract class EventConnector(sourceIdentifier: String) extends Connector(sourceIdentifier) with EventManager
