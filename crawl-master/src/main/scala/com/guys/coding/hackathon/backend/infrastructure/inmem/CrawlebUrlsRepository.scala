package com.guys.coding.hackathon.backend.infrastructure.inmem

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.functor.toFunctorOps
import simulacrum.typeclass

@typeclass
trait CrawlebUrlsRepository[F[_]] {
  def saveVisted(url: String): F[Unit]
  def isVisited(url: String): F[Boolean]
}
object CrawlebUrlsRepository {

  def instance[F[_]: Sync]: F[CrawlebUrlsRepository[F]] =
    Ref
      .of[F, Set[String]](Set.empty)
      .map(urls =>
        new CrawlebUrlsRepository[F] {

          override def saveVisted(url: String): F[Unit] = urls.update(_ + url)

          override def isVisited(url: String): F[Boolean] = urls.get.map(_.contains(url))
        }
      )

}
