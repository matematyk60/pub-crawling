package com.guys.coding.hackathon.backend

import java.security.PrivateKey
import java.security.PublicKey

import scala.concurrent.ExecutionContext
import cats.effect.ContextShift
import cats.effect.Timer
import com.guys.coding.hackathon.backend.api.graphql.core.GraphqlRoute
import com.guys.coding.hackathon.backend.domain.ExampleService
import com.guys.coding.hackathon.backend.infrastructure.jwt.JwtTokenService
import com.guys.coding.hackathon.backend.infrastructure.postgres.Database
import hero.common.logging.Logger
import hero.common.logging.slf4j.LoggingConfigurator
import cats.effect.{IO, Resource}
import com.guys.coding.hackathon.backend.infrastructure.KafkaRequestService
import neotypes.{GraphDatabase, Session}
import neotypes.cats.effect.implicits._
import org.neo4j.driver.AuthTokens
import hero.common.util.LoggingExt
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.syntax.kleisli._
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieJobRepository
import com.guys.coding.hackathon.proto.notifcation.Request

class Application(config: ConfigValues)(
    implicit ec: ExecutionContext,
    appLogger: Logger[IO],
    cs: ContextShift[IO]
) extends LoggingExt {

  LoggingConfigurator.setRootLogLevel(config.app.rootLogLevel)
  LoggingConfigurator.setLogLevel("com.guys.coding.hackathon.backend", config.app.appLogLevel)

  private val privateKey: PrivateKey = null //PrivateKeyReader.get(config.authKeys.privatePath)
  private val publicKey: PublicKey   = null //PublicKeyReader.get(config.authKeys.publicPath)
  private val jwtTokenService        = new JwtTokenService(publicKey, privateKey)

  def start()(implicit t: Timer[IO]): IO[Unit] = Database.transactor(config.postgres).use { tx =>

    val neo = config.neo4j
    val session: Resource[IO, Session[IO]] = for {
      driver  <- GraphDatabase.driver[IO](neo.url, AuthTokens.basic(neo.username, neo.password))
      session <- driver.session
    } yield session

    val kafkaP = fs2.kafka.producerResource[IO, String, Request](KafkaRequestService.producerSettings(config.kafkaBootstrapServers))

    kafkaP.use {
      case (producer) =>
        implicit val kafkaRequestService: KafkaRequestService[IO] = new KafkaRequestService[IO]("requests", producer)

        val services = Services(new ExampleService[IO] {}, jwtTokenService, tx, kafkaRequestService)
        val routes = Router(
          "/graphql" -> new GraphqlRoute(services).route
        ).orNotFound
        for {
          _ <- tx.trans.apply(DoobieJobRepository.Statements.createTable.run)
          _ <- appLogger.info(s"Started server at ${config.app.bindHost}:${config.app.bindPort}")
          _ <- BlazeServerBuilder[IO]
            .bindHttp(config.app.bindPort, config.app.bindHost)
            .withHttpApp(CORS(routes))
            .serve
            .compile
            .drain
        } yield ()
    }


  }

}
