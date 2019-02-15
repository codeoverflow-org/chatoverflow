object Bootstrap {

  def main(args: Array[String]): Unit = {
    println("Hello! I'm the bootstrap launcher.")
  }

  /*
  TODO:
  1. Write SBT-Logic to extract all libs from "sbt dependencyList"
  2. Write SBT-logic to transform and save this information as URL List
  3. Make the boostrap launcher download everything, create folder structures
     and launch chat overflow with custom classpath
  4. Make fat jar with only the scala lib inside (and the chat overflow files?)
   */

  /*
  Test content:
  http://central.maven.org/maven2/log4j/log4j/1.2.17/log4j-1.2.17.jar
[info] chatoverflow:chatoverflow_2.12:0.2
[info] chatoverflow-api:chatoverflow-api_2.12:1.0
[info] com.fasterxml.jackson.core:jackson-annotations:2.8.0
[info] com.fasterxml.jackson.core:jackson-core:2.8.4
[info] com.fasterxml.jackson.core:jackson-databind:2.8.4
[info] com.github.scopt:scopt_2.12:3.5.0
[info] com.google.guava:guava:20.0
[info] com.googlecode.juniversalchardet:juniversalchardet:1.0.3
[info] com.thoughtworks.paranamer:paranamer:2.8
[info] commons-codec:commons-codec:1.10
[info] commons-logging:commons-logging:1.2
[info] eu.medsea.mimeutil:mime-util:2.1.3
[info] javax.servlet:javax.servlet-api:3.1.0
[info] log4j:log4j:1.2.17
[info] net.bytebuddy:byte-buddy:1.6.11
[info] net.bytebuddy:byte-buddy-agent:1.6.11
[info] org.apache.commons:commons-lang3:3.6
[info] org.apache.httpcomponents:httpclient:4.5.3
[info] org.apache.httpcomponents:httpcore:4.4.6
[info] org.apache.httpcomponents:httpmime:4.5.3
[info] org.eclipse.jetty:jetty-http:9.4.6.v20170531
[info] org.eclipse.jetty:jetty-io:9.4.6.v20170531
[info] org.eclipse.jetty:jetty-security:9.4.6.v20170531
[info] org.eclipse.jetty:jetty-server:9.4.6.v20170531
[info] org.eclipse.jetty:jetty-servlet:9.4.6.v20170531
[info] org.eclipse.jetty:jetty-util:9.4.6.v20170531
[info] org.eclipse.jetty:jetty-webapp:9.4.6.v20170531
[info] org.eclipse.jetty:jetty-xml:9.4.6.v20170531
[info] org.hamcrest:hamcrest-core:1.3
[info] org.javassist:javassist:3.21.0-GA
[info] org.json4s:json4s-ast_2.12:3.5.2
[info] org.json4s:json4s-core_2.12:3.5.2
[info] org.json4s:json4s-jackson_2.12:3.5.2
[info] org.json4s:json4s-scalap_2.12:3.5.2
[info] org.mockito:mockito-core:2.7.22
[info] org.objenesis:objenesis:2.5
[info] org.pircbotx:pircbotx:2.1
[info] org.reflections:reflections:0.9.11
[info] org.scala-lang:scala-compiler:2.12.5
[info] org.scala-lang:scala-reflect:2.12.5
[info] org.scala-lang.modules:scala-parser-combinators_2.12:1.0.6
[info] org.scala-lang.modules:scala-xml_2.12:1.0.6
[info] org.scala-sbt:test-interface:1.0
[info] org.scalatra:scalatra-common_2.12:2.6.4
[info] org.scalatra:scalatra-json_2.12:2.6.3
[info] org.scalatra:scalatra-scalate_2.12:2.6.4
[info] org.scalatra:scalatra-specs2_2.12:2.6.4
[info] org.scalatra:scalatra-test_2.12:2.6.4
[info] org.scalatra:scalatra_2.12:2.6.4
[info] org.scalatra.scalate:scalate-core_2.12:1.8.0
[info] org.scalatra.scalate:scalate-util_2.12:1.8.0
[info] org.slf4j:slf4j-api:1.7.25
[info] org.slf4j:slf4j-log4j12:1.7.22
[info] org.specs2:classycle:1.4.3
[info] org.specs2:specs2-analysis_2.12:4.0.1
[info] org.specs2:specs2-common_2.12:4.0.1
[info] org.specs2:specs2-core_2.12:4.0.1
[info] org.specs2:specs2-fp_2.12:4.0.1
[info] org.specs2:specs2-matcher-extra_2.12:4.0.1
[info] org.specs2:specs2-matcher_2.12:4.0.1
[info] org.specs2:specs2-mock_2.12:4.0.1
   */

}
