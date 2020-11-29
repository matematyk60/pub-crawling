package com.guys.coding.hackathon.backend.infrastructure.redis

import cats.Functor
import com.guys.coding.hackathon.backend.domain.{EntityConfig, GlobalConfig, JobId}
import dev.profunktor.redis4cats.RedisCommands
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax.EncoderOps
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import cats.syntax.functor.toFunctorOps

class RedisConfigRepository[F[_]: Functor](cmd: RedisCommands[F, String, String]) {
  val key                                       = "crawling-config"
  def saveConfig(config: GlobalConfig): F[Unit] = cmd.set(key, config.asJson.spaces4)
  def getConfig(): F[Option[GlobalConfig]] =
    cmd.get(key).map(str => str.flatMap(str => globalConfigDecoder.decodeJson(Json.fromString(str)).toOption))
  def saveJobSelectedDomains(jobId: JobId, domains: List[String]): F[Unit] = cmd.set(jobId.value, domains.asJson.spaces4)
  def getJobSelectedDomains(jobId: JobId): F[Option[List[String]]] =
    cmd.get(jobId.value).map(str => str.flatMap(str => jobSelectedDomainsDecoder.decodeJson(Json.fromString(str)).toOption).map(_.domains))

  implicit val entityConfigEncoder: Encoder[EntityConfig] = deriveEncoder
  implicit val globalConfigEncoder: Encoder[GlobalConfig] = deriveEncoder
  implicit val entityConfigDecoder: Decoder[EntityConfig] = deriveDecoder
  implicit val globalConfigDecoder: Decoder[GlobalConfig] = deriveDecoder

  case class JobSelectedDomains(domains: List[String])

  implicit val jobSelectedDomainsEncoder: Encoder[JobSelectedDomains] = deriveEncoder
  implicit val jobSelectedDomainsDecoder: Decoder[JobSelectedDomains] = deriveDecoder
}

object RedisConfigRepository {
  def apply[F[_]](implicit i: RedisConfigRepository[F]): RedisConfigRepository[F] = i
}
