package org.codeoverflow.chatoverflow2.framework.helper

import java.net.{URL, URLClassLoader}

/**
  * This plugin class loader does only exist for plugin security policy checks.
  *
  * @param urls Takes an array of urls an creates a simple URLClassLoader with it
  */
class PluginClassLoader(urls: Array[URL]) extends URLClassLoader(urls)