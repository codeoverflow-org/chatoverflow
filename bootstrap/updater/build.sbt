name := "chatoverflow-bootstrap-updater"
version := "0.3" // Currently not used anywhere
assemblyJarName in assembly := "ChatOverflow.jar"

// JSON Lib to read the json provided by GitHub Releases
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.2"

// jansi is used to check if the application is running with a tty in a interactive session
libraryDependencies += "org.fusesource.jansi" % "jansi" % "1.18"

// Progressbar is used to display progress of update zip download
libraryDependencies += "me.tongfei" % "progressbar" % "0.7.4"

packageBin / includePom := false
