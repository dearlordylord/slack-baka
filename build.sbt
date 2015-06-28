import NativePackagerKeys._

packageArchetype.java_application

name := "Slack Baka"

version := "0.1"

scalaVersion := "2.11.6"

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases"
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "org.reactivemongo" %% "reactivemongo" % "0.11.0-M1"
)
