package com.guys.coding.hackathon.backend

import java.security.PrivateKey
import java.security.PublicKey

import scala.concurrent.ExecutionContext

import cats.effect.ContextShift
import cats.effect.IO
import cats.effect.Timer
import hero.common.logging.Logger
import hero.common.logging.slf4j.LoggingConfigurator
import hero.common.util.LoggingExt
import redis.RedisClient
import akka.actor.ActorSystem
import com.guys.coding.hackathon.backend.infrastructure.redis.RedisConfigService

class Application(config: ConfigValues)(
    implicit ec: ExecutionContext,
    appLogger: Logger[IO],
    cs: ContextShift[IO]
) extends LoggingExt {

  implicit private val system: ActorSystem = ActorSystem("CrawlerActorSystem", config.raw)

  LoggingConfigurator.setRootLogLevel(config.app.rootLogLevel)
  LoggingConfigurator.setLogLevel("com.guys.coding.hackathon.backend", config.app.appLogLevel)

  val redis = RedisClient(config.redis.host, config.redis.port)

  val configService = new RedisConfigService(redis)

  def start()(implicit t: Timer[IO]): IO[Unit] =
    ???

}
