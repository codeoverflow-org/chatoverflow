name := "chatoverflow-meta-build"
version := "3.0.0"
sbtPlugin := true

// With 2.12.5 the code doesn't compile when java >= 9 is used
// Check https://github.com/scala/scala-dev/issues/480 for more information on this
scalaVersion := "2.12.6"

// JSON lib (Jackson) used for parsing the GUI version in the package.json file
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.2"
