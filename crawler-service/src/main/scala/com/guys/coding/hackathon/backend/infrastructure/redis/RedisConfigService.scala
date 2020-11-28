package com.guys.coding.hackathon.backend.infrastructure.redis

import redis.RedisCommands
import com.guys.coding.hackathon.backend.domain._
import cats.effect.IO
import scala.concurrent.ExecutionContext
import cats.effect.ContextShift
import io.circe._
import io.circe.parser._
import io.circe.generic.semiauto._

class RedisConfigService(redis: RedisCommands)(implicit ec: ExecutionContext, cs: ContextShift[IO]) extends ConfigService[IO] {

  val key = "crawling-config"

  override def get(): IO[Option[CrawlingConfig]] = {
    IO.fromFuture {
      IO {
        redis
          .get(key)
          .map(_.flatMap(s => parse(s.utf8String).flatMap(_.as[CrawlingConfig]).toOption))
      }
    }
  }

  implicit val NamedEntityDecoder: Decoder[NamedEntity]       = deriveDecoder[NamedEntity]
  implicit val CrawlingConfigDecoder: Decoder[CrawlingConfig] = deriveDecoder[CrawlingConfig]

}
