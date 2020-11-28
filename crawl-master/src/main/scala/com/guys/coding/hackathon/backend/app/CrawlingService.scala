package com.guys.coding.hackathon.backend.app

import cats.Monad
import hero.common.util.IdProvider
import cats.syntax.functor.toFunctorOps
import cats.syntax.flatMap.toFlatMapOps
import com.guys.coding.hackathon.proto.notifcation.{Crawl, Query, Request}
import hero.common.util.time.TimeUtils.TimeProvider
import cats.syntax.traverse.toTraverseOps
import cats.instances.list.catsStdInstancesForList
import com.guys.coding.hackathon.backend.infrastructure.KafkaRequestService

object CrawlingService {

  def duckDuckGoUrl(phrases: List[String]): String = {
    val search = phrases.map(phrase => s"""$phrase""").mkString(" ")
    s"http://duckduckgo.com/html?q=$search"
  }

  def googleUrl(phrases: List[String]): String = {
    val search = phrases.map(phrase => s"""$phrase""").mkString(" ")
    s"http://duckduckgo.com/html?q=$search!g"
  }

  def startFromPhrases[F[_] : Monad : IdProvider : TimeProvider : KafkaRequestService](phrases: List[String], operator: String): F[Unit] = {
    val operator_ = operator.toLowerCase match {
      case "and" => Query.Operator.AND
      case "or" => Query.Operator.OR
      case _ => Query.Operator.AND
    }
      for {
        jobId <- IdProvider[F].newId()
        urls = List(googleUrl(phrases), duckDuckGoUrl(phrases))
        requests <- urls.traverse(url => IdProvider[F].newId().map(requestId => Request.Is.Crawl(Crawl(requestId, url, Some(Query(phrases, operator_)), jobId))).map(Request(_)))
        _ <- KafkaRequestService[F].sendRequests(requests)
      } yield ()
    }


}