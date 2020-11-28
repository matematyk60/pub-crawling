enablePlugins(SbtNativePackager)
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.3.1")
addCompilerPlugin("org.typelevel"  %% "kind-projector"     % "0.11.0" cross CrossVersion.full)
addCompilerPlugin("org.augustjune" %% "context-applied"    % "0.1.4")
addCompilerPlugin(scalafixSemanticdb)

val protoSettings = Seq(
  PB.targets in Compile := Seq(
    scalapb.gen(flatPackage = true) -> (sourceManaged in Compile).value
  )
)


val dockerSettings = Seq(
  dockerBaseImage := "openjdk:13-jdk-buster",
  daemonUser in Docker := "root",
  dockerRepository := Some("porcupine96"),
  dockerExposedPorts := Seq(8080)
)

lazy val `crawler-service` =
  (project in file("."))
    .settings(
      organization := "com.guys.coding",
      name := "crawler-service",
      scalaVersion := "2.13.3",
      resolvers ++= Dependencies.additionalResolvers,
      libraryDependencies ++= Dependencies.all,
      scalacOptions ++= CompilerOps.all,
      parallelExecution in Test := false
    )
    .settings(dockerSettings: _*)
    .settings(protoSettings: _*)

PB.protoSources in Compile := Seq(
  baseDirectory.value / "src" / "main" / "protobuf",
  baseDirectory.value / ".." / "protocol"
)
