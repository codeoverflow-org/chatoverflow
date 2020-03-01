// Command Line Parsing
libraryDependencies += "com.github.scopt" %% "scopt" % "3.7.1"

// log4j Logger
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.30"

// Scalatra (REST, ...)
libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % "2.7.0",
  "org.scalatra" %% "scalatra-scalate" % "2.7.0",
  "org.scalatra" %% "scalatra-specs2" % "2.7.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "provided",
  "org.eclipse.jetty" % "jetty-webapp" % "9.4.7.v20170914" % "provided",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
  "org.scalatra" %% "scalatra-json" % "2.7.0",
  "org.scalatra" %% "scalatra-swagger" % "2.7.0",
)

// JSON Lib (Jackson)
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.6.7"

// PIRCBotX
libraryDependencies += "org.pircbotx" % "pircbotx" % "2.1"

// Reflections API for annotation indexing
libraryDependencies += "org.reflections" % "reflections" % "0.9.11"

// Akka Actors
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.6.1",
  //"com.typesafe.akka" %% "akka-testkit" % "2.6.1" % Test
)

// Google GSON
libraryDependencies += "com.google.code.gson" % "gson" % "2.8.6"

// JDA
resolvers += "jcenter-bintray" at "https://jcenter.bintray.com"
libraryDependencies += "net.dv8tion" % "JDA" % "3.8.3_464"

// Serial Communication
libraryDependencies += "com.fazecast" % "jSerialComm" % "2.5.3"

// Socket.io
libraryDependencies += "io.socket" % "socket.io-client" % "1.0.0"

// Coursier
libraryDependencies += "io.get-coursier" %% "coursier" % "2.0.0-RC5-4"