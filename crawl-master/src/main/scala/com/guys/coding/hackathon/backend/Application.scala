package com.guys.coding.hackathon.backend

import java.security.PrivateKey
import java.security.PublicKey
import java.time.Clock
import java.util.UUID

import scala.concurrent.ExecutionContext
import cats.effect.ContextShift
import cats.effect.Timer
import com.guys.coding.hackathon.backend.api.graphql.core.GraphqlRoute
import com.guys.coding.hackathon.backend.domain.ExampleService
import com.guys.coding.hackathon.backend.infrastructure.jwt.JwtTokenService
import com.guys.coding.hackathon.backend.infrastructure.postgres.{transactions, Database, DoobieJobRepository, DoobieRequestRepository}
import hero.common.logging.Logger
import hero.common.logging.slf4j.LoggingConfigurator
import cats.effect.{IO, Resource}
import cats.implicits.catsSyntaxTuple2Parallel
import com.guys.coding.hackathon.backend.infrastructure.kafka.{KafkaRequestService, KafkaResponseSource}
import neotypes.{GraphDatabase, Session}
import neotypes.cats.effect.implicits._
import dev.profunktor.redis4cats.effect.Log.Stdout._
import org.neo4j.driver.AuthTokens
import hero.common.util.{IdProvider, LoggingExt}
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.syntax.kleisli._
import com.guys.coding.hackathon.backend.infrastructure.redis.RedisConfigRepository
import com.guys.coding.hackathon.proto.notifcation.Request
import cats.tagless.syntax.all.toFunctorKOps
import cats.~>
import com.guys.coding.hackathon.backend.app.ResponseProcessor
import com.guys.coding.hackathon.backend.infrastructure.inmem.CrawlebUrlsRepository
import dev.profunktor.redis4cats.Redis
import doobie.free.connection.ConnectionIO
import hero.common.util.time.TimeUtils.TimeProvider
import com.guys.coding.hackathon.backend.domain.EntityService
import com.guys.coding.hackathon.backend.infrastructure.neo4j.Neo4jNodeRepository

class Application(config: ConfigValues)(
    implicit ec: ExecutionContext,
    appLogger: Logger[IO],
    cs: ContextShift[IO],
    timer: Timer[IO]
) extends LoggingExt {

  LoggingConfigurator.setRootLogLevel(config.app.rootLogLevel)
  LoggingConfigurator.setLogLevel("com.guys.coding.hackathon.backend", config.app.appLogLevel)

  private val privateKey: PrivateKey                  = null //PrivateKeyReader.get(config.authKeys.privatePath)
  private val publicKey: PublicKey                    = null //PublicKeyReader.get(config.authKeys.publicPath)
  private val jwtTokenService                         = new JwtTokenService(publicKey, privateKey)
  implicit private val timeProvider: TimeProvider[IO] = TimeProvider.io(Clock.systemUTC())
  implicit private val idProvider: IdProvider[IO]     = IdProvider.io
  implicit private val kafkaResponseSource =
    new KafkaResponseSource[IO]("master", UUID.randomUUID().toString, config.kafkaBootstrapServers, "crawler-responses")
  def start(): IO[Unit] = Database.transactor(config.postgres).use { tx =>
    implicit val transformer: ConnectionIO ~> IO =
      transactions.doobieTransactorTransformer(tx)
    val neo = config.neo4j

    val kafkaPR = fs2.kafka.producerResource[IO, String, Request](KafkaRequestService.producerSettings(config.kafkaBootstrapServers))
    val redisR  = Redis.apply[IO].utf8(config.redisConnectionString)

    implicit val jobIOREPOSITORY: DoobieJobRepository[IO]         = DoobieJobRepository.mapK(transformer)
    implicit val requestIOREPOSITORY: DoobieRequestRepository[IO] = DoobieRequestRepository.mapK(transformer)

    val resources = for {
      driver  <- GraphDatabase.driver[IO](neo.url, AuthTokens.basic(neo.username, neo.password))
      session <- driver.session

      kafkaP <- kafkaPR
      redis  <- redisR
    } yield (kafkaP, redis, session)

    resources.use {
      case (producer, redis, session) =>
        implicit val kafkaRequestService: KafkaRequestService[IO]     = new KafkaRequestService[IO]("crawler-requests", producer)
        implicit val redisConfigRepository: RedisConfigRepository[IO] = new RedisConfigRepository[IO](redis)
        implicit val crawledUrlsRepository: CrawlebUrlsRepository[IO] = CrawlebUrlsRepository.instance[IO].unsafeRunSync()
        implicit val entityService                                    = EntityService.instance(new Neo4jNodeRepository(session))
        val services =
          Services(
            entityService,
            new ExampleService[IO] {},
            jwtTokenService,
            tx,
            kafkaRequestService,
            jobIOREPOSITORY,
            requestIOREPOSITORY,
            redisConfigRepository
          )
        val routes = Router(
          "/graphql" -> new GraphqlRoute(services).route
        ).orNotFound
        for {
          _ <- tx.trans.apply(DoobieJobRepository.Statements.createTable.run)
          _ <- tx.trans.apply(DoobieRequestRepository.Statements.createTable.run)
          _ <- appLogger.info(s"Started server at ${config.app.bindHost}:${config.app.bindPort}")
          run = (
            BlazeServerBuilder[IO]
              .bindHttp(config.app.bindPort, config.app.bindHost)
              .withHttpApp(CORS(routes))
              .serve
              .compile
              .drain,
            ResponseProcessor.run[IO].compile.drain
          ).parTupled
          _ <- run
        } yield ()
    }

  }

}
