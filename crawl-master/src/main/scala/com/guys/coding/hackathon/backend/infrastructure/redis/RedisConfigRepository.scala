package com.guys.coding.hackathon.backend.infrastructure.redis

import com.guys.coding.hackathon.backend.domain.{EntityConfig, GlobalConfig}
import dev.profunktor.redis4cats.RedisCommands
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import io.circe.generic.semiauto.deriveEncoder

class RedisConfigRepository[F[_]](cmd: RedisCommands[F, String, String]) {
  val key                                       = "crawling-config"
  def saveConfig(config: GlobalConfig): F[Unit] = cmd.set(key, config.asJson.spaces4)

  implicit val entityConfigEncoder: Encoder[EntityConfig] = deriveEncoder
  implicit val globalConfigEncoder: Encoder[GlobalConfig] = deriveEncoder
}

object RedisConfigRepository {
  def apply[F[_]](implicit i: RedisConfigRepository[F]): RedisConfigRepository[F] = i
}
