val scala3Version = "3.3.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "load-songs",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    // libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.32",//no logging
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.0",
    libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.13",
    libraryDependencies += "org.apache.httpcomponents" % "httpcore-nio" % "4.4.14",
    libraryDependencies += "org.elasticsearch.client" % "elasticsearch-rest-high-level-client" % "7.17.6",
    libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.34.0",
    libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.14.1",
    libraryDependencies += "org.json4s" %% "json4s-jackson" % "4.0.7",
    libraryDependencies += "org.deeplearning4j" % "deeplearning4j-nlp" % "1.0.0-M2.1",
    libraryDependencies += "org.deeplearning4j" % "deeplearning4j-ui" % "1.0.0-M2.1",
    libraryDependencies += "org.nd4j" % "nd4j-native-platform" % "1.0.0-M2.1",//CPU
    // libraryDependencies += "org.nd4j" % "nd4j-cuda-10.2-platform" % "1.0.0-beta7",//GPU
    libraryDependencies += "org.apache.lucene" % "lucene-core" % "8.11.0",
    libraryDependencies += "org.apache.lucene" % "lucene-suggest" % "8.11.0"
  )
