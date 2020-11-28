package com.guys.coding.hackathon.backend.domain

import cats.data.NonEmptyList
import cats.instances.option.catsStdInstancesForOption
import cats.syntax.traverse.toTraverseOps
import com.guys.coding.hackathon.backend.infrastructure.neo4j.Neo4jNodeRepository
import com.guys.coding.hackathon.proto.notifcation.EntityMatch
import mouse.all.anySyntaxMouse
import cats.effect.IO
import simulacrum.typeclass

@typeclass
trait EntityService[F[_]] {

  def saveReturning(jobId: JobId, entries: Seq[EntityMatch], urls: Seq[String]): F[Unit]
  def insertQueryNode(id: JobId, entityValue: EntityValue): F[Unit]
}

object EntityService {

  def instance(neo4J: Neo4jNodeRepository): EntityService[IO] = new EntityService[IO] {

    def saveReturning(jobId: JobId, entries: Seq[EntityMatch], urls: Seq[String]) = {
      val optTo = entries.toList.map(m => (EntityId(m.entityId), EntityValue(m.value))) |> (NonEmptyList.fromList _)
      optTo.traverse(to => neo4J.saveEdge(from = jobId, to = to, urls = urls.toList)).void
    }

    def insertQueryNode(id: JobId, entityValue: EntityValue): IO[Unit] =
      neo4J.insertNode(id, EntityId("query"), entityValue = entityValue)

  }

}
