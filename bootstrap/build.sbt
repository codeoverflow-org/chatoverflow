name := "chatoverflow-bootstrap"
version := "0.1"
assemblyJarName in assembly := "ChatOverflow.jar"

libraryDependencies += "org.jline" % "jline-terminal-jansi" % "3.11.0" // used for terminal width
libraryDependencies += "io.get-coursier" %% "coursier" % "2.0.0-RC3-2" // Coursier is used to download the deps of the framework
fork := true

packageBin / includePom := false