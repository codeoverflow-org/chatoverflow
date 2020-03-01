name := "chatoverflow-meta-build"
version := "4.0.0"
sbtPlugin := true

scalaVersion := "2.12.10"

// JSON lib (Jackson) used for parsing the GUI version in the package.json file
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.6.7"
