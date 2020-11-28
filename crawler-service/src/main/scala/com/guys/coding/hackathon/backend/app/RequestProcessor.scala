package com.guys.coding.hackathon.backend.app

import com.guys.coding.hackathon.backend.infrastructure.kafka.KafkaSource
import cats.effect.Concurrent
import hero.common.logging.Logger
import cats.syntax.flatMap.toFlatMapOps
import com.guys.coding.hackathon.proto.notifcation.Request
import com.guys.coding.hackathon.proto.notifcation.Response
import hero.common.fs2.StreamUtils.Implicits.StreamExt
import scala.concurrent.duration._
import cats.effect.Timer
import org.http4s._
import org.http4s.client.Client
import hero.common.util.time.TimeUtils.TimeProvider
import hero.common.logging.LoggingSupport
import fs2.kafka.CommittableOffsetBatch
import fs2.{Chunk, Stream}
import cats.Applicative
import cats.syntax.apply._
import com.guys.coding.hackathon.backend.domain.ConfigService
import com.guys.coding.hackathon.backend.domain.CrawlingService
import com.guys.coding.hackathon.backend.domain.ResponseService

object RequestProcessor extends LoggingSupport {

  def process[F[_]: KafkaSource[*[_], Request]: Concurrent: Logger: Timer: TimeProvider: ConfigService: ResponseService](
      implicit client: Client[F],
      ed: EntityDecoder[F, String]
  ): F[Unit] = {
    val run =
      KafkaSource[F, Request].source
        .map { partition =>
          partition
            .groupWithin(1000, 3.seconds)
            .evalMap { chunk =>
              val offset = CommittableOffsetBatch.fromFoldableMap(chunk)(_.offset)
              processEventsChunk[F](chunk.map(_.value), processSingle[F](_)) *> offset.commit
            }
        }
        .parJoinUnbounded
        .handleErrorWith { ex =>
          val logAndSleep = Logger[F].error(s"Error encountered in processing events stream, restarting in 5s", ex) *> Timer[F].sleep(5.seconds)
          Stream.eval(logAndSleep)
        }
        .repeat
        .compile
        .drain

    Logger[F].info("Starting request processor") *> run
  }

  def processEventsChunk[F[_]: Concurrent: Logger](records: Chunk[Request], processSingle: Request => F[Unit]) =
    Logger[F].info(s"Processing event chunk of size ${records.size}") *> Stream
      .chunk(records)
      .covary[F]
      .groupBy[Int](message => Applicative[F].pure(partition(message)))
      .map { case (_, messages) => messages.evalMap(processSingle) }
      .parJoinUnbounded
      .compile
      .lastOrError

  def processSingle[F[_]: KafkaSource[*[_], Request]: Concurrent: Logger: TimeProvider: ConfigService: ResponseService](
      request: Request
  )(implicit client: Client[F], ed: EntityDecoder[F, String]): F[Unit] = {
    import Request.Is._

    val handle =
      request.is match {
        case Empty =>
          throw new IllegalStateException("Request.Empty")

        case Crawl(crawl) =>
          ConfigService[F]
            .get()
            .flatMap {
              case Some(config) =>
                CrawlingService.crawl[F](crawl.requestId, crawl.query, crawl.url, config).flatMap {
                  case Right(success) =>
                    ResponseService[F].send(Response(Response.Is.Success(success)))

                  case Left(failure) =>
                    ResponseService[F].send(Response(Response.Is.Failure(failure)))
                }

              case None =>
                throw new IllegalStateException("Crawling config not found.")
            }
      }

    Logger[F].info(s"Processing $request") *> handle
  }

  private def partition(r: Request): Int = {
    import Request.Is._

    val key = r.is match {
      case Empty        => throw new IllegalStateException("Request.Is.Empty")
      case Crawl(crawl) => crawl.requestId
    }

    val parallelism = 32
    Math.abs(key.hashCode()) % parallelism
  }

}
