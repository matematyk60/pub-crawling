package com.guys.coding.hackathon.backend.app

import com.guys.coding.hackathon.backend.infrastructure.kafka.KafkaSource
import cats.effect.Concurrent
import hero.common.logging.Logger
import com.guys.coding.hackathon.proto.notifcation.Request
import com.guys.coding.hackathon.proto.notifcation.CrawlSuccess
import com.guys.coding.hackathon.proto.notifcation.Response
import hero.common.fs2.StreamUtils.Implicits.StreamExt
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.functor.toFunctorOps
import scala.concurrent.duration._
import cats.effect.Timer
import org.http4s._
import cats.syntax.applicativeError._
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
import pprint.pprintln
import com.guys.coding.hackathon.proto.notifcation.CrawlFailure

object RequestProcessor extends LoggingSupport {

  def process[F[_]: KafkaSource[*[_], Request]: Concurrent: Logger: Timer: TimeProvider: ConfigService: ResponseService](
      implicit client: Client[F],
      ed: EntityDecoder[F, String]
  ): F[Unit] = {
    val run =
      KafkaSource[F, Request].source
        .map { partition =>
          partition
            .groupWithin(100, 1.second)
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
                val response =
                  if (config.discardedJobs.contains(crawl.jobId)) {
                    F.pure(Response(Response.Is.Success(CrawlSuccess(crawl.requestId, Nil, Nil))))
                  } else {

                    CrawlingService
                      .crawl[F](crawl.requestId, crawl.query, crawl.url, config)
                      .map {
                        case Right(success) =>
                          pprintln(s"SUCCESS: ${success.urls.size} urls, ${success.foundEntities.size} entities.")
                          Response(Response.Is.Success(success))

                        case Left(failure) =>
                          pprintln("HANDLED FAILURE")
                          Response(Response.Is.Failure(failure))
                      }
                      .recoverWith { ex =>
                        Logger[F]
                          .error("Error while crawling", ex)
                          .as(Response(Response.Is.Failure(CrawlFailure(crawl.requestId))))
                      }
                  }

                response
                  .flatMap(ResponseService[F].send)
                  .recoverWith { ex =>
                    Logger[F].error("Error while sending response", ex)
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
