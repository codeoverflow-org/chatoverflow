package org.codeoverflow.chatoverflow.framework.helper

import java.net.{URL, URLClassLoader}

import org.codeoverflow.chatoverflow.WithLogger

/**
 * This plugin class loader is used for plugin security policy checks,
 * to expose the addURL method to the package inorder to add all required dependencies after dependency resolution
 * and most importantly to isolate the plugin from the normal classpath and only access the classpath if it needs to load the ChatOverflow api.
 * Also if this PluginClassLoader had access to the classpath the same classes of the classpath would have
 * higher priority over the classes in this classloader which could be a problem  if a plugin uses a newer version
 * of a dependency that the framework.
 *
 * @param urls Takes an array of urls an creates a simple URLClassLoader with it
 */
class PluginClassLoader(urls: Array[URL]) extends URLClassLoader(urls, PluginClassLoader.platformClassloader) {
  // Note the platform classloader in the constructor of the URLClassLoader as the parent.
  // That way the classloader skips the app classloader with the classpath when it is asks it's parents for classes.

  protected[helper] override def addURL(url: URL): Unit = super.addURL(url) // just exposes this method to be package-private instead of class internal protected

  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    if (name.startsWith("org.codeoverflow.chatoverflow.api")) {
      PluginClassLoader.appClassloader.loadClass(name) // Api needs to be loaded from the classpath
    } else {
      super.loadClass(name, resolve) // non api class. load it as normal
    }
  }
}

/**
 * This companion object holds references to the app classloader (normal classloader, includes java and classpath)
 * and to the extension/platform classloader depending on the java version that excludes the classpath,
 * but still includes everything from java.
 */
private object PluginClassLoader extends WithLogger {
  val appClassloader: ClassLoader = this.getClass.getClassLoader
  val platformClassloader: ClassLoader = {
    var current = appClassloader
    while (current != null && !current.getClass.getName.contains("ExtClassLoader") && // ExtClassLoader is java < 9
      !current.getClass.getName.contains("PlatformClassLoader")) { // PlatformClassLoader is java >= 9
      current = current.getParent
    }

    if (current != null) {
      current
    } else {
      logger error "Platform classloader couldn't be found. Falling back to normal app classloader."
      appClassloader
    }
  }
}