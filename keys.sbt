// Settings
pluginBuildFileName := "plugins.sbt"
pluginFolderNames := List("plugins-public", "plugins-private")
pluginTargetFolderNames := List("plugins", s"target/scala-${scalaVersion.value.split('.').dropRight(1).mkString(".")}/plugins")
apiProjectPath := "api"
guiProjectPath := "gui"



// Plugin framework setting keys
lazy val pluginBuildFileName = settingKey[String]("The filename of the plugin build file. Remember to gitignore it!")
lazy val pluginFolderNames = settingKey[List[String]]("The folder names of all plugin source directories. Remember to gitignore it!")
lazy val pluginTargetFolderNames = settingKey[List[String]]("The folder names of compiled and packaged plugins. Remember to gitignore these!")
lazy val apiProjectPath = settingKey[String]("The path to the api sub project. Remember to gitignore it!")
lazy val guiProjectPath = settingKey[String]("The path of the Angular gui. Remember to gitignore it!")

// Plugin framework task keys
lazy val create = TaskKey[Unit]("create", "Creates a new plugin. Interactive command using the console.")
lazy val fetch = TaskKey[Unit]("fetch", "Searches for plugins in plugin directories, builds the plugin build file.")
lazy val copy = TaskKey[Unit]("copy", "Copies all packaged plugin jars to the target plugin folder.")
lazy val deploy = TaskKey[Unit]("deploy", "Prepares the environment for deployment, fills deploy folder.")
lazy val deployDev = TaskKey[Unit]("deployDev", "Prepares the environment for plugin developers, fills deployDev folder.")



// Tasks

import org.codeoverflow.chatoverflow.build.BuildUtils.scalaMajorVersion
import org.codeoverflow.chatoverflow.build.FrameworkBuild._
import org.codeoverflow.chatoverflow.build.deployment.DeploymentUtility
import org.codeoverflow.chatoverflow.build.plugins.{PluginCreateWizard, PluginUtility}

create := new PluginCreateWizard(streams.value.log).createPluginTask(pluginFolderNames.value, PluginCreateWizard.getApiVersion.value)
fetch := new PluginUtility(streams.value.log).fetchPluginsTask(pluginFolderNames.value, pluginBuildFileName.value,
  pluginTargetFolderNames.value, apiProjectPath.value, guiProjectPath.value)
copy := new PluginUtility(streams.value.log).copyPluginsTask(pluginFolderNames.value, pluginTargetFolderNames.value, scalaMajorVersion.value)
deploy := DeploymentUtility.prepareDeploymentTask(streams.value.log, scalaMajorVersion.value)
deployDev := DeploymentUtility.prepareDevDeploymentTask(streams.value.log, scalaMajorVersion.value, apiProjectPath.value)

// Adds the gui production build as a dependency to both deployments, directly running uses the development build of the
// gui for performance reasons.
lazy val guiProdBuild = if(guiProject.isDefined) guiProject.get / deploy else Def.task{}
deploy := deploy.dependsOn(guiProdBuild).value
deployDev := deployDev.dependsOn(guiProdBuild).value

// Enhance existing functionality
fork in run := true // Start ChatOverflow in its own java process when starting it with 'sbt run'
