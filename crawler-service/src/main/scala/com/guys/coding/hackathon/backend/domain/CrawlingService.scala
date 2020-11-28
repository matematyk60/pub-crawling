package com.guys.coding.hackathon.backend.domain

import com.guys.coding.hackathon.proto.notifcation.Query
import org.http4s.client.Client
import org.http4s._
import com.guys.coding.hackathon.proto.notifcation.CrawlFailure
import com.guys.coding.hackathon.proto.notifcation.CrawlSuccess
import com.guys.coding.hackathon.backend.infrastructure.crawler.HttpClient
import cats.Applicative
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.functor.toFunctorOps
import cats.Monad
import com.guys.coding.hackathon.proto.notifcation.EntityMatch
import com.guys.coding.hackathon.proto.notifcation.Query.Operator

object CrawlingService {

  private val urlRegex           = raw"""[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*)""".r
  private val unwantedExtensions = Set("ico", "png", "jpg", "jpeg", "xml", "js")

  def crawl[F[_]: Monad](requestId: String, query: Option[Query], url: String, config: CrawlingConfig)(
      implicit client: Client[F],
      ed: EntityDecoder[F, String]
  ): F[Either[CrawlFailure, CrawlSuccess]] = {
    HttpClient
      .get[F](url)
      .flatMap {
        case Some(html) if matches(html, query) =>
          findEntities[F](html, config).map { entities =>
            Right(
              CrawlSuccess(
                requestId = requestId,
                urls = findLinks(html),
                foundEntities = entities
              )
            )
          }

        case Some(html) =>
          F.pure(
            Right(
              CrawlSuccess(
                requestId = requestId,
                urls = findLinks(html),
                foundEntities = Nil
              )
            )
          )

        case None =>
          F.pure(Left(CrawlFailure(requestId)))
      }
  }

  private def matches(html: String, queryOpt: Option[Query]): Boolean = {
    queryOpt match {
      case Some(Query(keywords, Operator.OR)) =>
        keywords.exists(html.contains)

      case Some(Query(keywords, Operator.AND)) =>
        keywords.forall(html.contains)

      case Some(_) | None => true
    }
  }

  // TODO: filter out 49.99
  // TODO: handle local links
  // TODO: unify domain case        (master?)
  // TODO: unify http / https / www (master?)

  private def findLinks(html: String): List[String] = {
    urlRegex
      .findAllIn(html)
      .map(dropLastSlash)
      .distinct
      .filterNot(url => unwantedExtensions.exists(url.endsWith))
      .filterNot(_.size < 5)
      .toList
  }

  private def dropLastSlash(url: String) =
    if (url.endsWith("/")) url.init
    else url

  private def findEntities[F[_]: Applicative](html: String, config: CrawlingConfig): F[List[EntityMatch]] = {
    F.pure {
      config.namedEntities.flatMap { entity =>
        val regex = entity.regex.r

        regex.findAllIn(html).toList.map { m =>
          EntityMatch(
            entityId = entity.entityId,
            value = m,
            count = 1 // TODO: groupby?
          )
        }
      }
    }
  }

}
