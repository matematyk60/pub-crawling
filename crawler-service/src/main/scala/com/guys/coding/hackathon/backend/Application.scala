package com.guys.coding.hackathon.backend

import scala.concurrent.ExecutionContext

import cats.effect.ContextShift
import cats.effect.IO
import cats.effect.Timer
import hero.common.logging.Logger
import hero.common.logging.slf4j.LoggingConfigurator
import hero.common.util.LoggingExt
import redis.RedisClient
import akka.actor.ActorSystem
import fs2.kafka.producerResource
import com.guys.coding.hackathon.backend.infrastructure.redis.RedisConfigService
import com.guys.coding.hackathon.backend.app.RequestProcessor
import com.guys.coding.hackathon.backend.infrastructure.kafka.KafkaRequestSource
import com.guys.coding.hackathon.backend.infrastructure.kafka.KafkaSource
import com.guys.coding.hackathon.proto.notifcation.Request
import java.{util => ju}
import hero.common.util.time.TimeUtils.TimeProvider
import org.http4s.client.middleware.FollowRedirect
import java.time.Clock
import com.guys.coding.hackathon.backend.infrastructure.kafka.KafkaResponseService
import com.guys.coding.hackathon.backend.domain.ResponseService
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.Client

class Application(config: ConfigValues)(
    implicit ec: ExecutionContext,
    appLogger: Logger[IO],
    cs: ContextShift[IO]
) extends LoggingExt {

  implicit private val system: ActorSystem = ActorSystem("CrawlerActorSystem", config.raw)

  implicit private val clock: Clock                   = Clock.systemUTC()
  implicit private val timer: Timer[IO]               = IO.timer(ec)
  implicit private val timeProvider: TimeProvider[IO] = TimeProvider.io

  LoggingConfigurator.setRootLogLevel(config.app.rootLogLevel)
  LoggingConfigurator.setLogLevel("com.guys.coding.hackathon.backend", config.app.appLogLevel)

  val redis = RedisClient(config.redis.host, config.redis.port)

  implicit val configService = new RedisConfigService(redis)

  implicit val requestSource: KafkaSource[IO, Request] =
    new KafkaRequestSource(
      groupId = "crawler-service",
      clientId = ju.UUID.randomUUID().toString,
      bootstrapServers = config.kafka.bootstrapServers,
      topic = "crawler-requests"
    )

  def start(): IO[Unit] = {
    val resources =
      for {
        client     <- BlazeClientBuilder[IO](ec).withMaxTotalConnections(256).resource
        responseKP <- producerResource(KafkaResponseService.producerSettings(config.kafka.bootstrapServers))
      } yield (client, responseKP)

    resources.use {
      case (client, responseKP) =>
        implicit val httpClient: Client[IO] = FollowRedirect(maxRedirects = 5)(client)

        implicit val responseService: ResponseService[IO] =
          new KafkaResponseService("crawler-responses", responseKP)

        RequestProcessor.process[IO].as(())
    }

  }

}
