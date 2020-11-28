package com.guys.coding.hackathon.backend.infrastructure.postgres
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime

import cats.syntax.functor.toFunctorOps
import cats.tagless.{Derive, FunctorK}
import com.guys.coding.hackathon.backend.domain.JobId
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieJobRepository.Job
import com.guys.coding.hackathon.proto.notifcation.Query.Operator
import doobie.implicits._
import doobie.implicits.javasql.TimestampMeta
import doobie.postgres.implicits._
import doobie.{implicits => _, _}
import simulacrum.typeclass

@typeclass
trait DoobieJobRepository[F[_]] {
  def getJobs(): F[List[Job]]
  def getJob(id: JobId): F[Option[Job]]
  def createJob(job: Job): F[Unit]
}

object DoobieJobRepository extends DoobieJobRepository[ConnectionIO] {

  implicit def functorKInstance: FunctorK[DoobieJobRepository] =
    Derive.functorK[DoobieJobRepository]

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

  private val makeJob: (String, Option[String], Int, String, Timestamp, String, List[String], Int) => Job = {
    case (
        jobId,
        parentJobId,
        depth,
        name,
        time,
        operator,
        phrases,
        iterations
        ) =>
      Job(
        JobId(jobId),
        parentJobId.map(JobId),
        depth,
        name,
        time.toLocalDateTime().atZone(ZoneId.systemDefault()),
        operatorFromString(operator),
        phrases,
        iterations
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
        |    iterations INT NOT NULL
        |);""".stripMargin.update

    def insertUser(job: Job) =
      Update[(String, Option[String], Int, String, Timestamp, String, List[String], Int)](
        "INSERT INTO public.jobs (id, parent_job_id, job_depth, name, start_time, operator, phrases, iterations) VALUES (?,?, ?, ?, ?, ? ,?)"
      ).toUpdate0(
        (
          job.id.value,
          job.parentJob.map(_.value),
          job.jobDepth,
          job.name,
          Timestamp.from(job.startTime.toInstant()),
          operatorToString(job.operator),
          job.phrases,
          job.iterations
        )
      )

    val getJobs =
      sql"SELECT id, parent_job_id, job_depth, name, start_time, operator, phrases, iterations FROM jobs"
        .query[(String, Option[String], Int, String, Timestamp, String, List[String], Int)]

    def getJob(id: JobId) =
      sql"SELECT id, parent_job_id, job_depth, name, start_time, operator, phrases, iterations jobs where id = ${id.value}"
        .query[(String, Option[String], Int, String, Timestamp, String, List[String], Int)]

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
      phrases: List[String],
      iterations: Int
  )

}
