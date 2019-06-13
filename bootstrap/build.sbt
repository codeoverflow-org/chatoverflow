name := "chatoverflow-bootstrap"
version := "0.1"
assemblyJarName in assembly := "ChatOverflow.jar"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.1.1"
libraryDependencies += "org.jline" % "jline-terminal-jansi" % "3.11.0" // used for terminal width
fork := true