package com.guys.coding.hackathon.backend.app

import cats.{Applicative, Monad}
import cats.data.OptionT
import cats.effect.Concurrent
import com.guys.coding.hackathon.backend.domain.{JobId, RequestId}
import com.guys.coding.hackathon.backend.infrastructure.kafka.{KafkaRequestService, KafkaResponseSource}
import com.guys.coding.hackathon.backend.infrastructure.postgres.{DoobieJobRepository, DoobieRequestRepository}
import com.guys.coding.hackathon.proto.notifcation.{Crawl, CrawlFailure, CrawlSuccess, EntityMatch, Query, Request, Response}
import cats.syntax.functor.toFunctorOps
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.apply.catsSyntaxApply
import com.guys.coding.hackathon.backend.infrastructure.inmem.CrawlebUrlsRepository
import hero.common.util.IdProvider
import cats.syntax.traverse.toTraverseOps
import cats.instances.list.catsStdInstancesForList
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieRequestRepository.{Request => DRequest}
import hero.common.util.time.TimeUtils.TimeProvider
import com.guys.coding.hackathon.backend.domain.EntityService
import com.guys.coding.hackathon.backend.infrastructure.redis.RedisConfigRepository

object ResponseProcessor {

  def run[F[_]: Concurrent: EntityService: KafkaResponseSource: KafkaRequestService: RedisConfigRepository: TimeProvider: IdProvider: DoobieRequestRepository: CrawlebUrlsRepository: DoobieJobRepository]
      : fs2.Stream[F, Unit] =
    KafkaResponseSource[F].source.map { partition =>
      partition.mapAsync(4) { record =>
        val response = record.record.value

        val action = response.is match {
          case Response.Is.Failure(CrawlFailure(requestId, _)) =>
            for {
              _       <- OptionT.liftF(DoobieRequestRepository[F].setRequestComplete(RequestId(requestId), success = false))
              request <- OptionT(DoobieRequestRepository[F].get(RequestId(requestId)))
              _       <- OptionT.liftF(CrawlebUrlsRepository[F].saveVisted(request.url))
            } yield ()

          case Response.Is.Success(CrawlSuccess(requestId, urls, foundEntities, _)) =>
            for {
              _               <- OptionT.liftF(DoobieRequestRepository[F].setRequestComplete(RequestId(requestId), success = true))
              request         <- OptionT(DoobieRequestRepository[F].get(RequestId(requestId)))
              _               <- OptionT.liftF(CrawlebUrlsRepository[F].saveVisted(request.url))
              job             <- OptionT(DoobieJobRepository[F].getJob(request.jobId))
              selectedDomains <- OptionT(RedisConfigRepository[F].getJobSelectedDomains(job.id))
              currentTime     <- OptionT.liftF(TimeProvider[F].currentTime())
              filteredUrls <- OptionT
                               .liftF(
                                 urls.toList.traverse(url => CrawlebUrlsRepository[F].isVisited(url).map(url -> _)).map(_.filterNot(_._2).map(_._1))
                               )
                               .filter(selectedDomains.contains(_) || selectedDomains.isEmpty)

              replacedFoundEntities = foundEntities.map {
                case e @ EntityMatch("phoneNumber", _, _, _) => e.copy(value = e.value.filter(_.isDigit))
                case e                                       => e
              }
              _ <- OptionT.liftF(EntityService[F].saveReturning(job.id, entries = replacedFoundEntities, urls = urls))

              _ <- OptionT.liftF(
                    if (request.depth == job.iterations) Applicative[F].unit
                    else
                      for {
                        requests <- filteredUrls.traverse(url =>
                                     for {
                                       requestId <- IdProvider[F].newId()
                                       request_  = Request.Is.Crawl(Crawl(requestId, url, Some(Query(job.phrases, job.operator)), job.id.value))
                                       _ <- DoobieRequestRepository[F].createRequest(
                                             DRequest(
                                               RequestId(requestId),
                                               url,
                                               parentRequest = Some(request.requestId),
                                               depth = request.depth + 1,
                                               JobId(job.id.value),
                                               currentTime
                                             )
                                           )
                                     } yield request_
                                   )
                        _ <- KafkaRequestService[F].sendRequests(requests.map(Request(_)))
                      } yield ()
                  )
            } yield ()

          case Response.Is.Empty => throw new IllegalArgumentException("Empty response")
        }
        action.value.void
      }
    }.parJoinUnbounded
}
