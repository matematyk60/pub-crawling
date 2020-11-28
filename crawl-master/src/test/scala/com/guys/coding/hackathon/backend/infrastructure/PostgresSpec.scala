package com.guys.coding.hackathon.backend.infrastructure

import java.time.Clock

import scala.concurrent.ExecutionContext.global

import _root_.doobie._
import _root_.doobie.free.connection
import _root_.doobie.free.connection.ConnectionIO
import _root_.doobie.implicits._
import _root_.doobie.util.transactor.Transactor.Aux
import cats.Id
import cats.arrow.FunctionK
import cats.effect.ContextShift
import cats.effect.IO
import com.guys.coding.hackathon.backend.ConfigValues.PostgresConfig
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieExampleRepository
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import hero.common.util.time.TimeUtils.TimeProvider
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec

trait PostgresSpec extends AnyFlatSpec with BeforeAndAfterAll {
  private val rawConfig: Config = ConfigFactory.load("test.conf")
  val config                    = PostgresConfig(rawConfig.getConfig("postgres"))

  implicit private val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timeProvider: TimeProvider[Id] =
    TimeProvider.id(Clock.systemUTC())

  val transactor: Aux[IO, Unit] =
    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      config.url,
      config.username,
      config.password
    )

  val ConnectionToId = new FunctionK[ConnectionIO, Id] {
    override def apply[A](fa: connection.ConnectionIO[A]): cats.Id[A] =
      fa.transact(transactor).unsafeRunSync()
  }

  override def beforeAll(): Unit = {
    val dropTables =
      for {
        _ <- sql"DROP TABLE IF EXISTS users".update.run
      } yield ()

    (
      dropTables.transact(transactor) *>
        transactor.trans.apply(DoobieExampleRepository.Statements.createTable.run)
    ).void.unsafeRunSync()
  }
}
