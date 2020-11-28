enablePlugins(SbtNativePackager)
enablePlugins(JavaAppPackaging)
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

enablePlugins(DockerPlugin)

val protoSettings = Seq(
  PB.targets in Compile := Seq(
    scalapb.gen(flatPackage = true) -> (sourceManaged in Compile).value
  )
)

val dockerSettings = Seq(
  dockerBaseImage := "openjdk:13-slim",
  daemonUser in Docker := "root",
  dockerRepository := Some("matematyk60"),
  dockerExposedPorts := Seq(8080)
)

addCompilerPlugin(scalafixSemanticdb)

lazy val `hackathon-backend` = (project in file("."))
  .settings(
    organization := "com.guys.coding",
    name := "crawl-master",
    scalaVersion := "2.13.3",
    resolvers ++= Dependencies.additionalResolvers,
    libraryDependencies ++= Dependencies.all,
    scalacOptions ++= CompilerOps.all,
    parallelExecution in Test := false
  )
  .settings(protoSettings: _*)
  .settings(dockerSettings: _*)

PB.protoSources in Compile :=
  Seq(
    baseDirectory.value / "../protocol/notification",
  )
