addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.9")


// JSON lib (Jackson) used for parsing the GUI version in the package.json file
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.2"