name := "chatoverflow-build"
sbtPlugin := true

// JSON lib (Jackson) used for parsing the GUI version in the package.json file
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.2"
