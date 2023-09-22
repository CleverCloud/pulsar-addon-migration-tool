lazy val akkaVersion = "2.6.20"
lazy val circeVersion = "0.14.3"
lazy val pulsar4sVersion = "2.9.0"

name := "pulsar-addon-migration-tool"

version := "0.2"

scalaVersion := "2.13.10"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-refined" % circeVersion,
  "io.circe" %% "circe-shapes" % circeVersion,
  "com.clever-cloud.pulsar4s" %% "pulsar4s-core" % pulsar4sVersion,
  "com.clever-cloud.pulsar4s" %% "pulsar4s-circe" % pulsar4sVersion,
  "com.clever-cloud.pulsar4s" %% "pulsar4s-avro" % pulsar4sVersion,
  "com.clever-cloud.pulsar4s" %% "pulsar4s-akka-streams" % pulsar4sVersion
)
