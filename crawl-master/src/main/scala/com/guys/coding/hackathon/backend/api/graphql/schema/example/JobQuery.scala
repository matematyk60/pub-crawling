package com.guys.coding.hackathon.backend.api.graphql.schema.example

import cats.effect.IO
import com.guys.coding.hackathon.backend.api.graphql.schema.QueryHolder
import com.guys.coding.hackathon.backend.api.graphql.service.GraphqlSecureContext
import com.guys.coding.hackathon.backend.domain.JobId
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieJobRepository
import doobie.util.transactor.Transactor
import sangria.schema._
import com.guys.coding.hackathon.backend.infrastructure.neo4j.Neo4jNodeRepository
import com.guys.coding.hackathon.backend.domain.EntityId

class JobQuery(neo4jRepo: Neo4jNodeRepository, tx: Transactor[IO]) extends QueryHolder {

  import ExampleTypes.{JobType, TableRowType}
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
      ), {

        val EntityIdArg = Argument("entityId", OptionInputType(StringType))
        val DepthArg    = Argument("jobDepth", OptionInputType(IntType))
        val LimitArg    = Argument("limit", IntType)
        val OffsetArg   = Argument("offset", IntType)

        Field(
          "entityTable",
          ListType(TableRowType),
          arguments = List(EntityIdArg, DepthArg, OffsetArg, LimitArg), // TODO:bcm  add parentJobId filter. Should be a list ideally
          resolve = ctx =>                                              // ctx.ctx.authorizedF { _ =>
          {

            neo4jRepo
              .getTable(
                ctx.arg(EntityIdArg).map(EntityId),
                ctx.arg(DepthArg),
                skip = ctx.arg(OffsetArg),
                limit = ctx.arg(LimitArg)
              )
              .unsafeToFuture()
          }
        )
      }
    )
}
