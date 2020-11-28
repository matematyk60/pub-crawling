package com.guys.coding.hackathon.backend

import com.typesafe.config.Config
import com.guys.coding.hackathon.backend.ConfigValues._

case class ConfigValues(
    app: ApplicationConfig,
    redis: RedisConfig,
    kafka: KafkaConfig,
    raw: Config
)

object ConfigValues {

  def apply(config: Config): ConfigValues = new ConfigValues(
    app = ApplicationConfig(
      bindHost = config.getString("bind-host"),
      bindPort = config.getInt("bind-port"),
      appLogLevel = config.getString("log-level.app"),
      rootLogLevel = config.getString("log-level.root")
    ),
    redis = RedisConfig(config.getConfig("redis")),
    kafka = KafkaConfig(config.getConfig("kafka")),
    raw = config
  )

  case class ApplicationConfig(
      bindHost: String,
      bindPort: Int,
      appLogLevel: String,
      rootLogLevel: String
  )

  case class RedisConfig(
      host: String,
      port: Int
  )

  object RedisConfig {
    def apply(config: Config): RedisConfig =
      RedisConfig(
        host = config.getString("host"),
        port = config.getInt("port")
      )
  }

  case class KafkaConfig(host: String, port: Int) {
    def bootstrapServers = s"$host:$port"
  }

  object KafkaConfig {
    def apply(config: Config): KafkaConfig =
      new KafkaConfig(
        host = config.getString("host"),
        port = config.getInt("port")
      )
  }
}
