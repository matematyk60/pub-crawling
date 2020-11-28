package com.guys.coding.hackathon.backend.infrastructure.postgres
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime

import cats.syntax.functor.toFunctorOps
import com.guys.coding.hackathon.backend.domain.JobId
import com.guys.coding.hackathon.proto.notifcation.Query.Operator
import doobie.implicits._
import doobie.implicits.javasql.TimestampMeta
import doobie.postgres.implicits._
import doobie.{implicits => _, _}

object DoobieJobRepository {

  def getJobs(): ConnectionIO[List[Job]] =
    Statements.getJobs
      .map(makeJob.tupled)
      .to[List]

  def getJob(id: JobId): ConnectionIO[Option[Job]] =
    Statements
      .getJob(id)
      .option
      .map(_.map(makeJob.tupled))

  def createJob(job: Job): ConnectionIO[Unit] =
    Statements.insertUser(job).run.void

  private val makeJob: (String, Option[String], Int, String, Timestamp, String, List[String]) => Job = {
    case (
        jobId,
        parentJobId,
        depth,
        name,
        time,
        operator,
        phrases
        ) =>
      Job(
        JobId(jobId),
        parentJobId.map(JobId),
        depth,
        name,
        time.toLocalDateTime().atZone(ZoneId.systemDefault()),
        operatorFromString(operator),
        phrases
      )
  }

  object Statements {

    val createTable = sql"""
        |CREATE TABLE public.jobs (
        |    id VARCHAR         NOT NULL PRIMARY KEY,
        |    parent_job_id      VARCHAR,
        |    job_depth          INTEGER NOT NULL,
        |    name               TEXT NOT NULL,
        |    start_time         TIMESTAMP NOT NULL,
        |    operator           VARCHAR NOT NULL,
        |    phrases            VARCHAR[] NOT NULL
        |);""".stripMargin.update

    def insertUser(job: Job) =
      Update[(String, Option[String], Int, String, Timestamp, String, List[String])](
        "INSERT INTO public.jobs (id, parent_job_id, job_depth, name, start_time, operator, phrases) VALUES (?,?, ?, ?, ?, ? ,?)"
      ).toUpdate0(
        (
          job.id.value,
          job.parentJob.map(_.value),
          job.jobDepth,
          job.name,
          Timestamp.from(job.startTime.toInstant()),
          operatorToString(job.operator),
          job.phrases
        )
      )

    val getJobs =
      sql"SELECT id, parent_job_id, job_depth, name, start_time, operator, phrases FROM jobs"
        .query[(String, Option[String], Int, String, Timestamp, String, List[String])]

    def getJob(id: JobId) =
      sql"SELECT id, parent_job_id, job_depth, name, start_time, operator, phrases jobs where id = ${id.value}"
        .query[(String, Option[String], Int, String, Timestamp, String, List[String])]

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

  case class Job(
      id: JobId,
      parentJob: Option[JobId],
      jobDepth: Int,
      name: String,
      startTime: ZonedDateTime,
      operator: Operator,
      phrases: List[String]
  )

}
