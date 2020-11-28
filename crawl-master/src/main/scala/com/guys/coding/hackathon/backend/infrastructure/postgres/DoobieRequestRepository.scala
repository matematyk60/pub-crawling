package com.guys.coding.hackathon.backend.infrastructure.postgres

import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime

import cats.syntax.functor.toFunctorOps
import com.guys.coding.hackathon.backend.domain.JobId
import com.guys.coding.hackathon.backend.domain.RequestId
import com.guys.coding.hackathon.proto.notifcation.Query.Operator
import doobie.implicits._
import doobie.implicits.javasql.TimestampMeta
import doobie.util.update.Update
import doobie.{implicits => _, _}

object DoobieRequestRepository {

  def jobRequests(jobId: JobId): ConnectionIO[List[Request]]          = Statements.jobsRequests(jobId).to[List]
  def childRequest(requestId: RequestId): ConnectionIO[List[Request]] = Statements.childRequests(requestId).to[List]
  def get(requestId: RequestId): ConnectionIO[Option[Request]]        = Statements.get(requestId).option

  def createRequest(request: Request): ConnectionIO[Unit] =
    Statements.insertRequest(request).run.void

  private val makeRequest: (String, String, Option[String], Int, String, Timestamp) => Request = {
    case (
        requestId,
        url,
        parentRequestId,
        depth,
        jobId,
        time
        ) =>
      Request(
        requestId = RequestId(requestId),
        url = url,
        parentRequest = parentRequestId.map(RequestId),
        depth = depth,
        jobId = JobId(jobId),
        startTime = time.toLocalDateTime().atZone(ZoneId.systemDefault())
      )
  }

  object Statements {

    val createTable = sql"""
        |CREATE TABLE public.requests (
        |    request_id         VARCHAR NOT NULL PRIMARY KEY,
        |    url                VARCHAR NOT NULL,
        |    parent_request_id  VARCHAR,
        |    depth              INTEGER NOT NULL,
        |    job_id             VARCHAR NOT NULL,
        |    start_time         TIMESTAMP NOT NULL
        |);""".stripMargin.update

    def insertRequest(r: Request) =
      Update[(String, String, Option[String], Int, String, Timestamp)](
        "INSERT INTO public.requests (request_id,url,parent_request_id,depth, job_id,start_time) VALUES (?,?,?,?, ?, ?)"
      ).toUpdate0(
        (
          r.requestId.value,
          r.url,
          r.parentRequest.map(_.value),
          r.depth,
          r.jobId.value,
          Timestamp.from(r.startTime.toInstant())
        )
      )

    def jobsRequests(j: JobId) =
      sql"SELECT request_id,url, parent_request_id, job_id,operator,phrases,start_time FROM requests where job_id = ${j.value}"
        .query[(String, String, Option[String], Int, String, Timestamp)]
        .map(makeRequest.tupled)

    def childRequests(parent: RequestId) =
      sql"SELECT request_id, url,parent_request_id, depth,job_id,start_time FROM requests where parent_request_id = ${parent.value}"
        .query[(String, String, Option[String], Int, String, Timestamp)]
        .map(makeRequest.tupled)

    def get(id: RequestId) =
      sql"SELECT request_id, url,parent_request_id, depth,job_id,start_time FROM requests where request_id = ${id.value}"
        .query[(String, String, Option[String], Int, String, Timestamp)]
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
      url: String,
      parentRequest: Option[RequestId],
      depth: Int,
      jobId: JobId, // send job to kafka
      startTime: ZonedDateTime
  )

}
