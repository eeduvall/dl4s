val scala3Version = "3.3.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "load-songs",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.13",
    libraryDependencies += "org.apache.httpcomponents" % "httpcore-nio" % "4.4.14",
    libraryDependencies += "org.elasticsearch.client" % "elasticsearch-rest-high-level-client" % "7.17.6",
    libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.34.0",
    libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.14.1",
    libraryDependencies += "org.json4s" %% "json4s-jackson" % "4.0.7"
  )
