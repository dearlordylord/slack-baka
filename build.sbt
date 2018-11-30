import NativePackagerKeys._

packageArchetype.java_application

name := "Slack Baka"

version := "0.2"

scalaVersion := "2.11.6"

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases"
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

libraryDependencies ++= {
  val akkaStreamV = "2.0.1"
  val akkadeps = Seq(
    "com.typesafe.akka" %% "akka-stream-experimental"             % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-core-experimental"          % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-experimental"               % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-xml-experimental"           % akkaStreamV
  )
  Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.3.9",
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
    "org.reactivemongo" %% "reactivemongo" % "0.12.6",
    "com.github.gilbertw1" %% "slack-scala-client" % "0.2.1", // .3
    "com.netaporter" %% "scala-uri" % "0.4.14"
  ) ++ akkadeps
}
