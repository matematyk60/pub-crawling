package com.guys.coding.hackathon.backend.infrastructure.postgres

import cats.effect.{Blocker, ContextShift, IO, Resource}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import com.guys.coding.hackathon.backend.ConfigValues.PostgresConfig

object Database {

  def transactor(config: PostgresConfig)(implicit cs: ContextShift[IO]): Resource[IO, HikariTransactor[IO]] =
    for {
      ec <- ExecutionContexts.fixedThreadPool[IO](config.connectionPoolSize)
      bl <- Blocker[IO]
      xa <- HikariTransactor.newHikariTransactor[IO](
             "org.postgresql.Driver",
             config.url,
             config.username,
             config.password,
             ec,
             bl
           )
    } yield xa
}
