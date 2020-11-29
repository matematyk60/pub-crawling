package com.guys.coding.hackathon.backend.app

import cats.Monad
import hero.common.util.IdProvider
import cats.syntax.functor.toFunctorOps
import cats.syntax.flatMap.toFlatMapOps
import com.guys.coding.hackathon.proto.notifcation.{Crawl, Query, Request}
import hero.common.util.time.TimeUtils.TimeProvider
import cats.syntax.traverse.toTraverseOps
import cats.instances.list.catsStdInstancesForList
import com.guys.coding.hackathon.backend.domain.{EntityConfig, GlobalConfig, JobId, RequestId}
import com.guys.coding.hackathon.backend.infrastructure.kafka.KafkaRequestService
import com.guys.coding.hackathon.backend.infrastructure.postgres.{DoobieJobRepository, DoobieRequestRepository}
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieJobRepository.Job
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieRequestRepository.{Request => DRequest}
import com.guys.coding.hackathon.backend.infrastructure.redis.RedisConfigRepository
import com.guys.coding.hackathon.backend.domain.EntityService
import com.guys.coding.hackathon.backend.domain.EntityValue
import com.guys.coding.hackathon.proto.notifcation.Query.Operator

object CrawlingService {

  def duckDuckGoUrl(phrases: List[String]): String = {
    val search = phrases.map(phrase => s"""$phrase""").mkString(" ")
    s"http://duckduckgo.com/html?q=${java.net.URLEncoder.encode(search)}"
  }

  def googleUrl(phrases: List[String]): String = {
    val search = phrases.map(phrase => s"""$phrase""").mkString(" ")
    s"http://duckduckgo.com/html?q=${java.net.URLEncoder.encode(search)}!g"
  }

  def startFromPhrases[F[_]: EntityService: Monad: IdProvider: TimeProvider: KafkaRequestService: DoobieJobRepository: DoobieRequestRepository: RedisConfigRepository](
      phrases: List[String],
      operator: String,
      jobIterations: Int,
      emailEntityEnabled: Boolean,
      phoneNumberEntityEnabled: Boolean,
      bitcoinAddressEnabled: Boolean,
      ssnNumberEnabled: Boolean,
      creditCardEnabled: Boolean
  ): F[JobId] = {
    val operator_ = operator.toLowerCase match {
      case "or" => Query.Operator.OR
      case _    => Query.Operator.AND
    }
    for {
      jobId       <- IdProvider[F].newId()
      currentTime <- TimeProvider[F].currentTime()
      job         = Job(JobId(jobId), parentJob = None, jobDepth = 0, "name1", currentTime, operator_, phrases, jobIterations)
      _ <- EntityService[F].insertQueryNode(
            job.id,
            job.jobDepth,
            EntityValue(operator + " (" + phrases.reduceOption(_ + " " + _).getOrElse("") + ")")
          )
      _    <- DoobieJobRepository[F].createJob(job)
      urls = List(googleUrl(phrases), duckDuckGoUrl(phrases))
      requests <- urls.traverse(url =>
                   for {
                     requestId <- IdProvider[F].newId()
                     request   = Request.Is.Crawl(Crawl(requestId, url, Some(Query(phrases, operator_)), jobId))
                     _ <- DoobieRequestRepository[F].createRequest(
                           DRequest(RequestId(requestId), url, parentRequest = None, depth = 0, JobId(jobId), currentTime)
                         )
                   } yield request
                 )
      enabledEntities = List(
        Option.when(emailEntityEnabled)(EntityConfig.emailEntity),
        Option.when(phoneNumberEntityEnabled)(EntityConfig.phoneNumber),
        Option.when(bitcoinAddressEnabled)(EntityConfig.bitcoinAddress),
        Option.when(ssnNumberEnabled)(EntityConfig.ssnNumber),
        Option.when(creditCardEnabled)(EntityConfig.creditCard)
      ).flatten
      globalConfig = GlobalConfig(enabledEntities, discardedJobs = List.empty)
      _            <- RedisConfigRepository[F].saveConfig(globalConfig)
      _            <- KafkaRequestService[F].sendRequests(requests.map(Request(_)))
    } yield JobId(jobId)
  }

  def crawlFromEntities[F[_]: EntityService: Monad: IdProvider: TimeProvider: KafkaRequestService: DoobieJobRepository: DoobieRequestRepository: RedisConfigRepository](
      jobId: JobId,
      choosenEntityValues: List[String],
      jobIterations: Int,
      emailEntityEnabled: Boolean,
      phoneNumberEntityEnabled: Boolean,
      bitcoinAddressEnabled: Boolean,
      ssnNumberEnabled: Boolean,
      creditCardEnabled: Boolean
  ): F[List[JobId]] = {
    for {
      currentTime <- TimeProvider[F].currentTime()
      parentJob   <- DoobieJobRepository[F].getJob(jobId).map(_.get)

      enabledEntities = List(
        Option.when(emailEntityEnabled)(EntityConfig.emailEntity),
        Option.when(phoneNumberEntityEnabled)(EntityConfig.phoneNumber),
        Option.when(bitcoinAddressEnabled)(EntityConfig.bitcoinAddress),
        Option.when(ssnNumberEnabled)(EntityConfig.ssnNumber),
        Option.when(creditCardEnabled)(EntityConfig.creditCard)
      ).flatten

      globalConfig = GlobalConfig(enabledEntities, discardedJobs = List.empty)
      _            <- RedisConfigRepository[F].saveConfig(globalConfig)

      jobIds <- choosenEntityValues
                 .traverse(enitiyValue =>
                   for {
                     newJobId <- IdProvider[F].newId().map(JobId)
                     phrases  = List(enitiyValue)
                     operator = Operator.AND

                     job = Job(
                       newJobId,
                       parentJob = Some(parentJob.id),
                       jobDepth = parentJob.jobDepth + 1,
                       "name1",
                       currentTime,
                       operator,
                       phrases,
                       jobIterations
                     )

                     _ <- EntityService[F].insertQueryNode(
                           job.id,
                           job.jobDepth,
                           EntityValue(enitiyValue)
                         )

                     _ <- DoobieJobRepository[F].createJob(job)

                     urls = List(googleUrl(phrases), duckDuckGoUrl(phrases))

                     requests <- urls.traverse(url =>
                                  for {
                                    requestId <- IdProvider[F].newId()
                                    request   = Request.Is.Crawl(Crawl(requestId, url, Some(Query(phrases, operator)), newJobId.value))
                                    _ <- DoobieRequestRepository[F].createRequest(
                                          DRequest(RequestId(requestId), url, parentRequest = None, depth = 0, newJobId, currentTime)
                                        )
                                  } yield request
                                )
                     _ <- KafkaRequestService[F].sendRequests(requests.map(Request(_)))

                   } yield jobId
                 )
    } yield jobIds
  }

}
