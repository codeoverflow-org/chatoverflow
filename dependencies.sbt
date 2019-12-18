// Command Line Parsing
libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0"

// log4j Logger
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.22"

// Scalatra (REST, ...)
libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % "2.6.5",
  "org.scalatra" %% "scalatra-scalate" % "2.6.5",
  "org.scalatra" %% "scalatra-specs2" % "2.6.5",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "provided",
  "org.eclipse.jetty" % "jetty-webapp" % "9.4.7.v20170914" % "provided",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "org.scalatra" %% "scalatra-json" % "2.6.3",
  "org.scalatra" %% "scalatra-swagger" % "2.6.5",
)

// JSON Lib (Jackson)
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.2"

// PIRCBotX
libraryDependencies += "org.pircbotx" % "pircbotx" % "2.1"

// Reflections API for annotation indexing
libraryDependencies += "org.reflections" % "reflections" % "0.9.11"

// Akka Actors
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.18",
  //"com.typesafe.akka" %% "akka-testkit" % "2.5.18" % Test
)

// Google GSON
libraryDependencies += "com.google.code.gson" % "gson" % "2.8.5"

// JDA
resolvers += "jcenter-bintray" at "https://jcenter.bintray.com"
libraryDependencies += "net.dv8tion" % "JDA" % "3.8.3_463"

// Serial Communication
libraryDependencies += "com.fazecast" % "jSerialComm" % "2.5.1"

// Socket.io
libraryDependencies += "io.socket" % "socket.io-client" % "1.0.0"

// Coursier
libraryDependencies += "io.get-coursier" %% "coursier" % "2.0.0-RC3-2"

// Twitter
libraryDependencies += "com.danielasfregola" %% "twitter4s" % "6.2"