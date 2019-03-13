object Bootstrap {

  def main(args: Array[String]): Unit = {
    println("Hello! I'm the bootstrap launcher.")
  }

  /*
  TODO:
  1. Write SBT-Logic to extract all libs from "sbt dependencyList" DONE
  2. Write SBT-logic to transform and save this information as URL List DONE
  2a. Make the logik copy the built jars into an bin folder inside the resources of the bootstrap
  3. Make the boostrap launcher download everything, create folder structures
     and launch chat overflow with custom classpath
  4. Make fat jar with only the scala lib inside (and the chat overflow files?)
   */

}
