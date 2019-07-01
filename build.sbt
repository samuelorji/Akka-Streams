name := "Akka-Streams"

version := "0.1"

scalaVersion := "2.12.8"

val akkaVersion      = "2.5.16"
val akkaHttpVersion  = "10.1.5"
val scalaTestVersion = "3.0.5"

libraryDependencies ++= Seq (
  "com.typesafe.akka"      %% "akka-actor"           % akkaVersion,
  "com.typesafe.akka"      %% "akka-slf4j"           % akkaVersion,
  "com.typesafe.akka"      %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.akka"      %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka"      %% "akka-stream"          % akkaVersion,
  "io.spray"               %% "spray-json"           % "1.3.3",
  "com.typesafe.akka"      %% "akka-stream-testkit"  % akkaVersion,
  "com.typesafe.akka"      %% "akka-testkit"         % akkaVersion,
  "org.scalatest"          %% "scalatest"            % scalaTestVersion
)