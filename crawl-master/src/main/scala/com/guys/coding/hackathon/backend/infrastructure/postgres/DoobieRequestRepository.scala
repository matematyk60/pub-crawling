package com.guys.coding.hackathon.backend.infrastructure.postgres

import java.sql.Timestamp
import java.time.ZoneId
import com.guys.coding.hackathon.backend.domain.{JobId, RequestId}
import cats.syntax.functor.toFunctorOps

import doobie.util.update.Update
import doobie.postgres.implicits._
import java.time.ZonedDateTime

import com.guys.coding.hackathon.proto.notifcation.Query.Operator
import doobie.implicits._
import doobie.implicits.javasql.TimestampMeta
import doobie.{implicits => _, _}

object DoobieRequestRepository {

  def jobRequests(jobId: JobId): ConnectionIO[List[Request]]          = Statements.jobsRequests(jobId).to[List]
  def childRequest(requestId: RequestId): ConnectionIO[List[Request]] = Statements.childRequests(requestId).to[List]
  def get(requestId: RequestId): ConnectionIO[Option[Request]]        = Statements.get(requestId).option

  def createRequest(request: Request): ConnectionIO[Unit] =
    Statements.insertRequest(request).run.void

  private val makeRequest: (String, Option[String], String, String, List[String], Timestamp) => Request = {
    case (
        requestId,
        parentRequestId,
        jobId,
        operator,
        phrases,
        time
        ) =>
      Request(
        RequestId(requestId),
        parentRequestId.map(RequestId),
        JobId(jobId),
        operatorFromString(operator),
        phrases,
        time.toLocalDateTime().atZone(ZoneId.systemDefault())
      )
  }

  object Statements {

    val createTable = sql"""
        |CREATE TABLE public.requests (
        |    request_id         VARCHAR NOT NULL PRIMARY KEY,
        |    parent_request_id  VARCHAR,
        |    job_id             VARCHAR NOT NULL,
        |    operator           VARCHAR NOT NULL,
        |    phrases            VARCHAR[] NOT NULL,
        |    start_time         TIMESTAMP NOT NULL
        |);""".stripMargin.update

    def insertRequest(r: Request) =
      Update[(String, Option[String], String, String, List[String], Timestamp)](
        "INSERT INTO public.requests (request_id,parent_request_id, job_id,operator,phrases,start_time) VALUES (?,?,?, ?, ?, ?)"
      ).toUpdate0(
        (
          r.requestId.value,
          r.parentRequest.map(_.value),
          r.jobId.value,
          operatorToString(r.operator),
          r.phrases,
          Timestamp.from(r.startTime.toInstant())
        )
      )

    def jobsRequests(j: JobId) =
      sql"SELECT request_id, parent_request_id, job_id,operator,phrases,start_time FROM requests where job_id = ${j.value}"
        .query[(String, Option[String], String, String, List[String], Timestamp)]
        .map(makeRequest.tupled)

    def childRequests(parent: RequestId) =
      sql"SELECT request_id, parent_request_id, job_id,operator,phrases,start_time FROM requests where parent_request_id = ${parent.value}"
        .query[(String, Option[String], String, String, List[String], Timestamp)]
        .map(makeRequest.tupled)

    def get(id: RequestId) =
      sql"SELECT request_id, parent_request_id, job_id,operator,phrases,start_time FROM requests where request_id = ${id.value}"
        .query[(String, Option[String], String, String, List[String], Timestamp)]
        .map(makeRequest.tupled)
  }

  val operatorToString: Operator => String = {
    case Operator.AND             => "and"
    case Operator.OR              => "or"
    case Operator.Unrecognized(e) => throw new IllegalArgumentException(s"Unrecognised operator proto $e")
  }

  val operatorFromString: String => Operator = {
    case "and" => Operator.AND
    case "or"  => Operator.OR
    case other => throw new IllegalArgumentException(s"Unrecognised operator string $other")
  }

  case class Request(
      requestId: RequestId,
      parentRequest: Option[RequestId],
      jobId: JobId,
      operator: Operator,
      phrases: List[String],
      startTime: ZonedDateTime
  )

}
