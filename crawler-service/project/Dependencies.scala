object Dependencies {
  import sbt._
  import scalapb.compiler.Version

  val CodeheroesCommonsVersion = "0.153"
  val ScalaTestVersion         = "3.2.3"
  val ScalaMockVersion         = "5.0.0"
  val SimulacrumVersion        = "0.19.0"
  val TypesafeConfigVersion    = "1.4.1"

  val grpcNettyVersion: String   = Version.grpcJavaVersion
  val grpcRuntimeVersion: String = Version.scalapbVersion

  val Http4sVersion      = "0.21.13"
  val CirceVersion       = "0.13.0"
  val CirceOpticsVersion = "0.13.0"

  val RedisVersion    = "1.9.0"
  val Fs2KafkaVersion = "1.0.0"

  val PprintVersion = "0.5.7"

  private val http4sDependencies = Seq(
    "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
    "org.http4s" %% "http4s-circe"        % Http4sVersion,
    "org.http4s" %% "http4s-dsl"          % Http4sVersion,
    "io.circe"   %% "circe-generic"       % CirceVersion,
    "io.circe"   %% "circe-parser"        % CirceVersion,
    "io.circe"   %% "circe-optics"        % CirceOpticsVersion
  )

  private val grpcDependencies = Seq(
    "io.grpc"              % "grpc-netty"            % grpcNettyVersion,
    "com.thesamet.scalapb" %% "scalapb-runtime"      % scalapb.compiler.Version.scalapbVersion % "protobuf",
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % grpcRuntimeVersion
  )

  private val miscDependencies = Seq(
    "io.codeheroes"        %% "commons-core" % CodeheroesCommonsVersion,
    "io.codeheroes"        %% "commons-fs2"  % CodeheroesCommonsVersion,
    "com.github.mpilquist" %% "simulacrum"   % SimulacrumVersion,
    "com.typesafe"         % "config"        % TypesafeConfigVersion,
    "com.lihaoyi"          %% "pprint"       % PprintVersion
  )

  private val redisDependencies = Seq(
    "com.github.etaty" %% "rediscala" % RedisVersion
  )

  private val kafkaDependencies = Seq(
    "com.github.fd4s" %% "fs2-kafka" % Fs2KafkaVersion
  )

  private val testDependencies = Seq(
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
  )

  val all: Seq[ModuleID] = Seq(
    http4sDependencies,
    grpcDependencies,
    redisDependencies,
    kafkaDependencies,
    testDependencies,
    miscDependencies
  ).flatten

  val additionalResolvers: Seq[Resolver] = Seq(
    Resolver.bintrayRepo("codeheroes", "maven"),
    Resolver.typesafeRepo("releases")
  )

}
