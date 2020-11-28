package com.guys.coding.hackathon.backend.infrastructure.postgres

import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import com.guys.coding.hackathon.backend.domain.{JobId,RequestId}
import cats.syntax.functor.toFunctorOps
import doobie.implicits._
import doobie.implicits.javasql.TimestampMeta
import doobie.{implicits => _, _}

object DoobieRequestRepository {

  // def getUsers(): ConnectionIO[List[Job]] =
  //   Statements.getJobs
  //     .map {
  //       case (id, name, last, birth) => Job(id, name, last, birth.toLocalDateTime().atZone(ZoneId.systemDefault()))
  //     }
  //     .to[List]

  // def createJob(id: String, name: String, initialRequestId: String, startTime: ZonedDateTime): ConnectionIO[Unit] =
  //   Statements.insertUser(id, name, initialRequestId, startTime).run.void

  object Statements {

    val createTable = sql"""
        |CREATE TABLE public.requests (
        |    request_id      VARCHAR NOT NULL PRIMARY KEY,
        |    job_id          VARCHAR NOT NULL,
        |    operator        VARCHAR NOT NULL,
        |    context         VARCHAR[]                  ,
        |    start_time      TIMESTAMP NOT NULL
        |);""".stripMargin.update

    def insertUser(
        id: String,
        name: String,
        initialRequestId: String,
        startTime: ZonedDateTime
    ) =
      Update[(String, String, String, Timestamp)](
        "INSERT INTO public.jobs (id,name,initial_request_id,start_time) VALUES (?,?, ?, ?)"
      ).toUpdate0((id, name, initialRequestId, Timestamp.from(startTime.toInstant())))

    val getJobs =
      sql"SELECT id,name,initial_request_id,start_time FROM users".query[(String, String, String, Timestamp)]
  }

  case class Request(
      requestId: RequestId,
      jobId: JobId,
      operator:String,
      startTime: ZonedDateTime
  )

}
