package com.guys.coding.hackathon.backend

import cats.effect.{ExitCode, IO, IOApp}
import com.typesafe.config.ConfigFactory
import hero.common.logging.Logger
import hero.common.logging.slf4j.ScalaLoggingLogger
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp {

  implicit private val logger: Logger[IO] = new ScalaLoggingLogger
  val app                                 = new Application(ConfigValues(ConfigFactory.load("app.conf")))
  override def run(args: List[String]): IO[ExitCode] =
    IO.asyncF(_ => app.start())

}
