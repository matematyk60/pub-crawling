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
import com.guys.coding.hackathon.backend.infrastructure.neo4j.Neo4jNodeRepository.TableRow

class Neo4jNodeRepository(session: Session[IO]) {
  // import com.guys.coding.hackathon.backend.infrastructure.neo4j.Neo4jNodeRepository.{enitityToMapper, EntityTo}

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

  def truncate(): IO[Unit] =
    // c"create(e:Entity{jobId:${id.value},jobDepth: ${jobDepth}, entityId: ${entityId.value}, entityValue: ${entityValue.value}});".query[Unit].single(session)
    for {
      _ <- c"match (a)-[r]-(b) delete a,r,b".query[Unit].single(session)
      _ <- c"match (a) delete a".query[Unit].single(session)
    } yield ()

  def makeEntityAJob(id: JobId, jobDepth: Int, entityValue: EntityValue): IO[Unit] =
    // c"create(e:Entity{jobId:${id.value},jobDepth: ${jobDepth}, entityId: ${entityId.value}, entityValue: ${entityValue.value}});".query[Unit].single(session)
    c"""MERGE(e:Entity{entityValue:${entityValue.value}})
     ON CREATE SET  e = {jobId:${id.value}, entityId: "unknown", entityValue: ${entityValue.value}, jobDepth: ${jobDepth}}
     ON MATCH SET e = {jobId:${id.value}, jobDepth: ${jobDepth}};
  """.query[Unit].single(session)

  def insertNode(id: JobId, jobDepth: Int, entityId: EntityId, entityValue: EntityValue): IO[Unit] =
    // c"create(e:Entity{jobId:${id.value},jobDepth: ${jobDepth}, entityId: ${entityId.value}, entityValue: ${entityValue.value}});".query[Unit].single(session)
    c"""MERGE(e:Entity{entityValue:${entityValue.value}})
     ON CREATE SET e = {jobId:${id.value},entityId: ${entityId.value}, entityValue: ${entityValue.value}, jobDepth: ${jobDepth}}
     ON MATCH SET e = {jobId:${id.value},entityId: ${entityId.value}, entityValue: ${entityValue.value}, jobDepth: ${jobDepth}};
  """.query[Unit].single(session)

  def saveEdge(from: JobId, to: NonEmptyList[(EntityId, EntityValue)]): IO[Unit] = {
    println(s"Saving edges from $from, to $to")
    to.traverse {
      case (id, value) =>
        c"""
      match(e:Entity{jobId:${from.value}})
          merge (e)-[r:coexists]-> (to:Entity {entityId: ${id.value}, entityValue:${value.value}, name : ${value.value}})
          ON CREATE SET r.counter = 1
          ON MATCH SET
          r.counter = coalesce(r.counter, 0) + 1;
    """.query[Unit].single(session)
    }.void

  }

  def getTable(
      jobIds: Option[List[JobId]],
      entityId: Option[EntityId],
      depth: Option[Int],
      skip: Int,
      limit: Int
  ): IO[List[Neo4jNodeRepository.TableRow]] = {

    val targetFilters =
      List(
        entityId.map(d => c"entityId: ${d.value}")
      ).flatten.reduceOption(_ + c", " + _).map(c"{" + _ + c"}").getOrElse(c"")

    val srcTest =
      List(
        depth.map(d => c"jobDepth: $d")
      ).flatten.reduceOption(_ + c", " + _).map(c"{" + _ + c"}").getOrElse(c"")

    val sourceFilters =
      jobIds.filter(_.nonEmpty).map(ids => c"and j.jobId IN ${ids.map(_.value)}").getOrElse(c"")

    (c"match (j:Entity" + srcTest + c") -[r:coexists]->(e:Entity" + targetFilters + c") where j.entityValue <> e.entityValue " + sourceFilters + c""" return
            j.jobId             as startJobId,
            j.entityValue       as startEntityValue,
            e.entityId          as foundEntityId,
            e.entityValue       as foundEntityValue,
            e.jobId             as foundEntityJobId,
            r.counter           as counter ORDER BY counter desc
            skip $skip limit $limit""")
      .query[TableRow]
      .list(session)
  }

  def getTableCount(
      jobIds: Option[List[JobId]],
      entityId: Option[EntityId],
      depth: Option[Int]
  ): IO[Int] = {

    val targetFilters =
      List(
        entityId.map(d => c"entityId: ${d.value}")
      ).flatten.reduceOption(_ + c", " + _).map(c"{" + _ + c"}").getOrElse(c"")

    val srcTest =
      List(
        depth.map(d => c"jobDepth: $d")
      ).flatten.reduceOption(_ + c", " + _).map(c"{" + _ + c"}").getOrElse(c"")

    val sourceFilters =
      jobIds.filter(_.nonEmpty).map(ids => c"where j.jobId IN ${ids.map(_.value)}").getOrElse(c"")

    (c"match (j:Entity" + srcTest + c") -[r:coexists]->(e:Entity" + targetFilters + c")" + sourceFilters + c""" return count(*)""")
      .query[Int]
      .single(session)

  }

// match (j:Entity) -[r:coexists]->(e:Entity{entityId:"email"}) return j.jobId as startJobId,j.entityValue as startValue,e.entityId,e.entityValue as foundEntityValue,r.counter

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
object Neo4jNodeRepository {
  case class TableRow(
      startJobId: String,
      startEntityValue: String,
      foundEntityId: String,
      foundEntityValue: String,
      foundEntityJobId: Option[String],
      counter: Int
  )

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
