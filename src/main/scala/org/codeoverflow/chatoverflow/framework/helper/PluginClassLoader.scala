package org.codeoverflow.chatoverflow.framework.helper

import java.net.{URL, URLClassLoader}

/**
 * This plugin class loader does only exist for plugin security policy checks and
 * to expose the addURL method to the package inorder to add all required dependencies after dependency resolution.
 *
 * @param urls Takes an array of urls an creates a simple URLClassLoader with it
 */
class PluginClassLoader(urls: Array[URL]) extends URLClassLoader(urls) {
  protected[helper] override def addURL(url: URL): Unit = super.addURL(url) // just exposes this method to be package-private instead of class internal protected
}