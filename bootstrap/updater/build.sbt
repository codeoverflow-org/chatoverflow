name := "chatoverflow-bootstrap-updater"
version := "0.3" // Currently not used anywhere
assemblyJarName in assembly := "ChatOverflow.jar"

// JSON Lib to read the json provided by GitHub Releases
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.2"

// JLine is used for terminal width and to check, if the application is running with a tty and interactive
libraryDependencies += "org.jline" % "jline-terminal-jansi" % "3.11.0"

packageBin / includePom := false
