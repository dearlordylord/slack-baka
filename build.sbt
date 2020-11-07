import com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging

enablePlugins(JavaServerAppPackaging)

name := "Slack Baka"

version := "0.2.1"

scalaVersion := "2.13.3"

resolvers += ("Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases")
resolvers += ("Akka Snapshot Repository" at "https://repo.akka.io/snapshots/")

libraryDependencies ++= {
//  val akkaV       = "2.4.1"
  val akkaStreamV = "2.6.10"
  val akkaHttpV = "10.2.1"
//  val scalaTestV  = "2.2.5"
  val akkadeps = Seq(
    "com.typesafe.akka" %% "akka-stream"                          % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-core"          % akkaHttpV,
    "com.typesafe.akka" %% "akka-http"               % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-spray-json"    % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-xml"           % akkaHttpV
  )
  Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.6.10",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
    "org.reactivemongo" %% "reactivemongo" % "1.0.0",
    "com.github.slack-scala-client" %% "slack-scala-client" % "0.2.11",
//    "io.netty" % "netty" % "3.6.3.Final" force(),
    "io.lemonlabs" %% "scala-uri" % "3.0.0",
    "org.dispatchhttp" %% "dispatch-core" % "1.2.0"
    //"org.mongodb.scala" %% "mongo-scala-driver" % "2.9.0"
  ) ++ akkadeps
}
