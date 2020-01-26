import org.codeoverflow.chatoverflow.build.BuildUtils.publishToGPR

publishTo := publishToGPR("chatoverflow")
publishMavenStyle := true

// This special task is needed, because the framework is aggregated with the api, gui and all plugins
// and the normal publish would thereof be executed on all these project instead of only the framework.
lazy val publishFramework = TaskKey[Unit]("publishFramework", "Publishes the framework to GPR")

publishFramework := {
  (Project("root", file(".")) / publish).value
}

