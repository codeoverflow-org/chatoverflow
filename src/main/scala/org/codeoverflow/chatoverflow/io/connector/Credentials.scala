package org.codeoverflow.chatoverflow.io.connector

trait Credentials {

}

case class TwitchCredentials(name: String, password: String) extends Credentials
