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

        val entries = NonEmptyList.of(
          (EntityId("phone"), EntityValue("123123123")),
          (EntityId("email"), EntityValue("ala@makota.pl")),
          (EntityId("PESEL"), EntityValue("99999999999999")),
          (EntityId("VIN"), EntityValue("???VIN???")),
          (EntityId("email"), EntityValue("ola@ula.pl"))
        )

        val startId = JobId("start")
        for {
          _ <- repo.insertNode(startId, EntityId("query"), EntityValue("IPHONE AND TANIO"))
          _ <- repo.saveEdge(startId, entries, urls = urls)
          _ <- IO(println("FINISH"))
        } yield ()

      }
      .unsafeRunSync()

  }

}
