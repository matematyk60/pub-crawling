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

  val Http4sVersion       = "0.21.13"
  val CirceVersion        = "0.13.0"
  val CirceOpticsVersion  = "0.13.0"
  val SangriaCirceVersion = "1.3.1"
  val SangriaVersion      = "2.0.0-M3"
  val JwtVersion          = "4.3.0"

  val doobieVersion           = "0.9.4"
  val enumeratumDoobieVersion = "1.6.0"

  private val http4sDependencies = Seq(
    "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
    "org.http4s" %% "http4s-circe"        % Http4sVersion,
    "org.http4s" %% "http4s-dsl"          % Http4sVersion,
    "io.circe"   %% "circe-generic"       % CirceVersion,
    "io.circe"   %% "circe-optics"        % CirceOpticsVersion
  )

  private val jwtDependencies = Seq(
    "com.pauldijou" %% "jwt-circe" % JwtVersion
  )

  private val sangriaDependencies = Seq(
    "org.sangria-graphql" %% "sangria-circe"   % SangriaCirceVersion,
    "io.codeheroes"       %% "commons-sangria" % CodeheroesCommonsVersion
  )

  private val postgresDependencies = Seq(
    "org.tpolecat" %% "doobie-postgres"   % doobieVersion,
    "org.tpolecat" %% "doobie-hikari"     % doobieVersion,
    "com.beachape" %% "enumeratum-doobie" % enumeratumDoobieVersion,
    "org.tpolecat" %% "doobie-scalatest"  % doobieVersion % Test
  )

  private val grpcDependencies = Seq(
    "io.grpc"              % "grpc-netty"            % grpcNettyVersion,
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % grpcRuntimeVersion
  )

  private val miscDependencies = Seq(
    "io.codeheroes"        %% "commons-core" % CodeheroesCommonsVersion,
    "com.github.mpilquist" %% "simulacrum"   % SimulacrumVersion,
    "com.typesafe"         % "config"        % TypesafeConfigVersion,
    "org.neo4j.driver" % "neo4j-java-driver" % "4.1.1"
  )

  private val testDependencies = Seq(
    "org.scalatest" %% "scalatest" % ScalaTestVersion % Test
    // "org.scalamock" %% "scalamock" % ScalaMockVersion % Test
  )

  val all: Seq[ModuleID] = Seq(
    http4sDependencies,
    sangriaDependencies,
    jwtDependencies,
    grpcDependencies,
    postgresDependencies,
    testDependencies,
    miscDependencies
  ).flatten

  val additionalResolvers: Seq[Resolver] = Seq(
    Resolver.bintrayRepo("codeheroes", "maven"),
    Resolver.typesafeRepo("releases")
  )

}
