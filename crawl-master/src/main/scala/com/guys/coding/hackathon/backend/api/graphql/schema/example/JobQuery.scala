package com.guys.coding.hackathon.backend.api.graphql.schema.example

import cats.effect.IO
import com.guys.coding.hackathon.backend.api.graphql.schema.QueryHolder
import com.guys.coding.hackathon.backend.api.graphql.service.GraphqlSecureContext
import com.guys.coding.hackathon.backend.domain.ExampleService
import com.guys.coding.hackathon.backend.domain.JobId
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieJobRepository
import doobie.util.transactor.Transactor
import sangria.schema._

class JobQuery(tx: Transactor[IO]) extends QueryHolder {

  import ExampleTypes.JobType
  val JobIdArg = Argument("jobId", StringType)

  override def queryFields(): List[Field[GraphqlSecureContext, Unit]] =
    fields[GraphqlSecureContext, Unit](
      Field(
        "job",
        OptionType(JobType),
        arguments = JobIdArg :: Nil,
        resolve = ctx => {
          tx.trans
            .apply(
              DoobieJobRepository.getJob(JobId(ctx.arg(JobIdArg)))
            )
            .unsafeToFuture()
        }
      ),
      Field(
        "jobs",
        ListType(JobType),
        arguments = Nil,
        resolve = _ => // ctx.ctx.authorizedF { _ =>
        {
          tx.trans
            .apply(
              DoobieJobRepository.getJobs()
            )
            .unsafeToFuture()
        }
      )
    )
}
