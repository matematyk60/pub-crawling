package com.guys.coding.hackathon.backend.infrastructure
import scala.concurrent.ExecutionContext.Implicits.global

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.Resource
import com.guys.coding.hackathon.backend.domain.EntityId
import com.guys.coding.hackathon.backend.domain.EntityValue
import com.guys.coding.hackathon.backend.domain.JobId
import com.guys.coding.hackathon.backend.infrastructure.neo4j.Neo4jNodeRepository
import neotypes.GraphDatabase
import neotypes.Session
import neotypes.cats.effect.implicits._
import org.neo4j.driver.AuthTokens
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.guys.coding.hackathon.backend.infrastructure.neo4j.Neo4jNodeRepository.TableRow

class Neo4jTest extends AnyFlatSpec with Matchers {
  implicit val cs = IO.contextShift(global)

  val session: Resource[IO, Session[IO]] = for {
    driver  <- GraphDatabase.driver[IO]("bolt://localhost:7687", AuthTokens.basic("neo4j", "123"))
    session <- driver.session
  } yield session

  val urls = List(
    "https://example.com",
    "https://allegro.pl",
    "https://youtube.com",
    "https://youtube.pl"
  )

  "Neo4JRepo" should "kinda work" in {
    session
      .use { s =>
        val repo = new Neo4jNodeRepository(s)

        val run1 = {
          val entries = NonEmptyList.of(
            (EntityId("phone"), EntityValue("123123123")),
            (EntityId("email"), EntityValue("ala@makota.pl")),
            (EntityId("PESEL"), EntityValue("99999999999999")),
            (EntityId("VIN"), EntityValue("???VIN???")),
            (EntityId("email"), EntityValue("ola@ula.pl"))
          )

          val startId = JobId("start")
          for {
            _ <- repo.insertNode(startId, 0, EntityId("query"), EntityValue("dzialaj start"))
            _ <- repo.saveEdge(startId, entries)
            _ <- IO(println("FINISH"))
          } yield ()
        }

        val run2 = {

          val entries = NonEmptyList.of(
            (EntityId("phone"), EntityValue("123123123")),
            (EntityId("email"), EntityValue("ala@makota.pl")),
            (EntityId("PESEL"), EntityValue("4424242")),
            (EntityId("VIN"), EntityValue("vini")),
            (EntityId("email"), EntityValue("ola@a.pl"))
          )

          val startId = JobId("oldsdmsdsfdsxxxxxxxxxd")
          for {
            _ <- repo.insertNode(startId, 0, EntityId("query"), EntityValue("dzialaj prosze"))
            _ <- repo.saveEdge(startId, entries)
            _ <- IO(println("FINISH"))
          } yield ()
        }

        val run3 = {
          val entries = NonEmptyList.of(
            (EntityId("phone"), EntityValue("444444")),
            (EntityId("VIN"), EntityValue("vini"))
          )

          val startId = JobId("ola@a.pl")
          for {
            _ <- repo.insertNode(startId, 1, EntityId("email"), EntityValue("ola@a.pl"))
            _ <- repo.saveEdge(startId, entries)
            _ <- IO(println("FINISH"))
          } yield ()
        }

        for {
          _ <- run1
          _ <- run2
          _ <- run3
        } yield ()

      }
      .unsafeRunSync()

  }

  it should "print my query" in {
    import neotypes.implicits.syntax.cypher.neotypesSyntaxCypherStringInterpolator
    import neotypes.implicits.mappers.all._

    def getTable(
        jobIds: Option[List[JobId]],
        entityId: Option[EntityId],
        depth: Option[Int],
        skip: Int,
        limit: Int
    ) = {

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

      (c"match (j:Entity" + srcTest + c") -[r:coexists]->(e:Entity" + targetFilters + c")" + sourceFilters + c""" return
            j.jobId             as startJobId,
            j.entityValue       as startEntityValue,
            e.entityId          as foundEntityId,
            e.entityValue       as foundEntityValue,
            r.counter           as counter
            skip $skip limit $limit""")
        .query[TableRow]
        .query
    }

    println(getTable(Some(List("ala", "ola").map(JobId)), None, Some(0), 0, 10))

  }

}
