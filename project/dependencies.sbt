lazy val build = ProjectRef(file("../build"), "build")
lazy val metaProject = (project in file(".")).dependsOn(build)
