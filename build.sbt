import com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging

enablePlugins(JavaServerAppPackaging)

name := "Slack Baka"

version := "0.2.1"

// scalaVersion := "2.13.3"
scalaVersion := "2.12.15" // temporarily to accommodate to API build


resolvers += ("Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases")
resolvers += ("Akka Snapshot Repository" at "https://repo.akka.io/snapshots/")

libraryDependencies ++= {
//  val akkaV       = "2.4.1"
  val akkaStreamV = "2.6.17"
  val akkaHttpV = "10.2.8"
//  val scalaTestV  = "2.2.5"
  val akkadeps = Seq(
    "com.typesafe.akka" %% "akka-stream"                          % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-core"          % akkaHttpV,
    "com.typesafe.akka" %% "akka-http"               % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-spray-json"    % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-xml"           % akkaHttpV
  )
  Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.6.17",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.0.0",
    "org.reactivemongo" %% "reactivemongo" % "1.0.7",
    //"com.github.slack-scala-client" %% "slack-scala-client-custom" % "0.3.0",
//    "io.netty" % "netty" % "3.6.3.Final" force(),
    "io.lemonlabs" %% "scala-uri" % "3.6.0",
    "org.dispatchhttp" %% "dispatch-core" % "1.2.0"
    //"org.mongodb.scala" %% "mongo-scala-driver" % "2.9.0"
  ) ++ akkadeps
}

val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

// point to the latest commit at the moment; they haven't updated their build yet
lazy val slackClientCustom = RootProject(uri("https://github.com/slack-scala-client/slack-scala-client.git#9a04762df15913f366d810a865e6f9a0b0edc779"))

val main = Project(id = "slack-baka", base = file(".")).dependsOn(slackClientCustom)