package com.guys.coding.hackathon.backend.app

import cats.Applicative
import cats.data.OptionT
import cats.effect.Concurrent
import cats.instances.list.catsStdInstancesForList
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.functor.toFunctorOps
import cats.syntax.traverse.toTraverseOps
import com.guys.coding.hackathon.backend.domain.EntityService
import com.guys.coding.hackathon.backend.domain.JobId
import com.guys.coding.hackathon.backend.domain.RequestId
import com.guys.coding.hackathon.backend.domain.UrlFilter
import com.guys.coding.hackathon.backend.infrastructure.inmem.CrawlebUrlsRepository
import com.guys.coding.hackathon.backend.infrastructure.kafka.KafkaRequestService
import com.guys.coding.hackathon.backend.infrastructure.kafka.KafkaResponseSource
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieJobRepository
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieRequestRepository
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieRequestRepository.{Request => DRequest}
import com.guys.coding.hackathon.proto.notifcation.Crawl
import com.guys.coding.hackathon.proto.notifcation.CrawlFailure
import com.guys.coding.hackathon.proto.notifcation.CrawlSuccess
import com.guys.coding.hackathon.proto.notifcation.EntityMatch
import com.guys.coding.hackathon.proto.notifcation.Query
import com.guys.coding.hackathon.proto.notifcation.Request
import com.guys.coding.hackathon.proto.notifcation.Response
import hero.common.util.IdProvider
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

          case Response.Is.Success(CrawlSuccess(requestId, rawUrls, foundEntities, _)) =>
            val urls = rawUrls.filter(UrlFilter.allowedToCrawl)
            for {
              _               <- OptionT.liftF(DoobieRequestRepository[F].setRequestComplete(RequestId(requestId), success = true))
              request         <- OptionT(DoobieRequestRepository[F].get(RequestId(requestId)))
              _               <- OptionT.liftF(CrawlebUrlsRepository[F].saveVisted(request.url))
              job             <- OptionT(DoobieJobRepository[F].getJob(request.jobId))
              selectedDomains <- OptionT.liftF(RedisConfigRepository[F].getJobSelectedDomains(job.id))
              currentTime     <- OptionT.liftF(TimeProvider[F].currentTime())
              filteredUrls <- OptionT
                               .liftF(
                                 urls.toList
                                   .traverse(url => CrawlebUrlsRepository[F].isVisited(url).map(url -> _))
                                   .map(_.filterNot(_._2).map(_._1))
                                   .map(_.filter(url => selectedDomains.exists(url.contains) || selectedDomains.isEmpty))
                               )

              replacedFoundEntities = foundEntities.map {
                case e @ EntityMatch("phoneNumber", _, _, _) => e.copy(value = e.value.filter(_.isDigit))
                case e                                       => e
              }
              _ <- OptionT.liftF(EntityService[F].saveReturning(job.id, entries = replacedFoundEntities))

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
