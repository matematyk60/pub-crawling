package com.guys.coding.hackathon.backend.infrastructure.neo4j

import java.time.ZonedDateTime

import cats.data.NonEmptyList
import cats.effect.IO
import com.guys.coding.hackathon.backend.domain.EntityId
import com.guys.coding.hackathon.backend.domain.EntityValue
import com.guys.coding.hackathon.backend.domain.JobId
import neotypes.Session
import neotypes.implicits.mappers.all._
import neotypes.implicits.syntax.cypher.neotypesSyntaxCypherStringInterpolator

class Neo4jNodeRepository(session: Session[IO]) {
  // import com.guys.coding.hackathon.backend.infrastructure.neo4j.Neo4jNodeRepository.{enitityToMapper, EntityTo}

  // TODO:bcm  connect entity to request
  // https://neotypes.github.io/neotypes/
  // NOTE: neotypes.generic is scheduled for 0.16.0, now use  neotypes.implicits.mappers.all._ instead.

  // def getPepole(): IO[List[(String, Int)]] = "match (p:Person) return p.name, p.born limit 10".query[(String, Int)].list(session)

  // TODO 1. Treat urls as edge paprameter
  // TODO 2. Add coexist nodes betwee onther
  // TODO Add domains as nodes
  // TODO (maybe) 2. What if url is a node?

  /* Nodes:
   *  - Entity
   *  - Domain
   *
   * Edges:
   * - coexists : Entity <-> Entity (urls, _FromJob_)
   * - ?? count
   * */

  def insertNode(id: JobId, entityId: EntityId, entityValue: EntityValue): IO[Unit] =
    c"""MERGE(e:Entity{jobId:${id.value}})
      ON CREATE SET e = {jobId:${id.value},entityId: ${entityId.value}, entityValue: ${entityValue.value}};
   """.query[Unit].single(session)
  // c"create(e:Entity{jobId:${id.value},entityId: ${entityId.value}, entityValue: ${entityValue.value}});".query[Unit].single(session)

  def saveEdge(from: JobId, to: NonEmptyList[(EntityId, EntityValue)], urls: List[String]): IO[Unit] = {
    to.traverse {
      case (id, value) => // TODO:bcm  batch it
        c"""
      match(e:Entity{jobId:${from.value}})
          merge (e)-[r:coexists]-> (to:Entity {entityId: ${id.value}, entityValue:${value.value}, name : ${value.value}})
          ON CREATE SET r.counter = ${urls.size}, r.urls = ${urls}
          ON MATCH SET
          r.counter = coalesce(r.counter, 0) + ${urls.size},
          r.urls = r.urls + ${urls}
    """.query[Unit].single(session)
    }.void

    // val x = c"""
    //   unwind ${to} as t
    //   with t
    //   match(e:Entity{jobId:${from.value}})
    //       merge (e)-[r:coexists]-> (to:Entity {entityId: t.entityId, entityValue:t.entitiyValue})
    //       ON CREATE SET r.counter = ${urls.size};
    //       ON MATCH SET
    //       r.counter = coalesce(n.counter, 0) + ${urls.size};
    // """

  }

}
object Neo4jNodeRepository {

  case class EntityNode(
      entityId: String,    //  albo found albo Query
      entityValue: String, //repr

      // non-leaf
      jobId: Option[JobId],
      jobStart: Option[ZonedDateTime]
  )

  case class EntityEndge( // Entity -> Entiy
      urls: List[String],
      count: Int
  )

}
