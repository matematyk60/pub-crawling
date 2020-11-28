package com.guys.coding.hackathon.backend

import com.typesafe.config.Config
import com.guys.coding.hackathon.backend.ConfigValues._

case class ConfigValues(
    app: ApplicationConfig,
    postgres: PostgresConfig,
    // authKeys: AuthKeys,
    raw: Config
)

object ConfigValues {

  def apply(config: Config): ConfigValues = new ConfigValues(
    ApplicationConfig(
      bindHost = config.getString("bind-host"),
      bindPort = config.getInt("bind-port"),
      appLogLevel = config.getString("log-level.app"),
      rootLogLevel = config.getString("log-level.root")
    ),
    PostgresConfig(config.getConfig("postgres")),
    // AuthKeys(config.getString("keys.private"), config.getString("keys.public")),
    config
  )

  case class AuthKeys(privatePath: String, publicPath: String)
  case class ApplicationConfig(
      bindHost: String,
      bindPort: Int,
      appLogLevel: String,
      rootLogLevel: String
  )

  case class PostgresConfig(
      host: String,
      port: Int,
      database: String,
      username: String,
      password: String,
      connectionPoolSize: Int
  ) {
    def url: String = s"jdbc:postgresql://$host:$port/$database"
  }

  object PostgresConfig {
    def apply(config: Config): PostgresConfig = PostgresConfig(
      host = config.getString("host"),
      port = config.getInt("port"),
      database = config.getString("database"),
      username = config.getString("username"),
      password = config.getString("password"),
      connectionPoolSize = config.getInt("connection-pool-size")
    )
  }
}
