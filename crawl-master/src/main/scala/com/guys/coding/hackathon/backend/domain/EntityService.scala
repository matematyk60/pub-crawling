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

  def saveReturning(jobId: JobId, entries: Seq[EntityMatch]): F[Unit]
  def insertQueryNode(id: JobId, jobDepth: Int, entityValue: EntityValue): F[Unit]
  def makeEntityAJob(id: JobId, jobDepth: Int, entityValue: EntityValue): F[Unit]
  def neo4j: Neo4jNodeRepository
}

object EntityService {

  def instance(neo4J: Neo4jNodeRepository): EntityService[IO] = new EntityService[IO] {

    override def neo4j: Neo4jNodeRepository = neo4J

    def saveReturning(jobId: JobId, entries: Seq[EntityMatch]) = {
      val optTo = entries.toList.map(m => (EntityId(m.entityId), EntityValue(m.value))) |> (NonEmptyList.fromList _)
      optTo.traverse(to => neo4J.saveEdge(from = jobId, to = to)).void
    }

    def insertQueryNode(id: JobId, jobDepth: Int, entityValue: EntityValue): IO[Unit] =
      neo4J.insertNode(id, jobDepth = jobDepth, EntityId("query"), entityValue = entityValue)

    def makeEntityAJob(id: JobId, jobDepth: Int, entityValue: EntityValue): IO[Unit] =
      neo4J.makeEntityAJob(id, jobDepth = jobDepth, entityValue = entityValue)
  }

}
