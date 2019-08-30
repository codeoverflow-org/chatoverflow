name := "chatoverflow-bootstrap"
version := "0.1"
assemblyJarName in assembly := "ChatOverflow.jar"

// JLine is used for terminal width
libraryDependencies += "org.jline" % "jline-terminal-jansi" % "3.11.0"

// Coursier is used to download the deps of the framework
// Excluding argonaut and it's dependencies because we don't use any json with Coursier and that way we are able
// to reduce the assembly jar file size from about 17MB (way too big) to only 8,8 MB, which is acceptable.
libraryDependencies += "io.get-coursier" %% "coursier" % "2.0.0-RC3-2" excludeAll(
  ExclusionRule(organization = "org.scala-lang", name = "scala-reflect"),
  ExclusionRule(organization = "io.argonaut", name = "argonaut_2.12"),
  ExclusionRule(organization = "com.chuusai", name = "shapeless_2.12"),
  ExclusionRule(organization = "com.github.alexarchambault", name = "argonaut-shapeless_6.2_2.12")
)

fork := true

packageBin / includePom := false