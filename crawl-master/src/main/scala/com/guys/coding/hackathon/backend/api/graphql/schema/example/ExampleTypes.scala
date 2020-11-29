package com.guys.coding.hackathon.backend.api.graphql.schema.example

import sangria.macros.derive._
import sangria.schema._
import com.guys.coding.hackathon.backend.api.graphql.service.GraphqlSecureContext
import com.guys.coding.hackathon.backend.domain.{JobId, RequestId}
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieJobRepository.Job
import hero.common.util.time.TimeUtils
import java.time.ZonedDateTime
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieRequestRepository.Request
import hero.common.sangria.ProtoEnumType
import com.guys.coding.hackathon.proto.notifcation.Query.Operator
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieRequestRepository
import com.guys.coding.hackathon.backend.infrastructure.neo4j.Neo4jNodeRepository.TableRow

object ExampleTypes {
  implicit val ZonedScalar =
    ScalarAlias[ZonedDateTime, Long](LongType, TimeUtils.zonedDateTimeToMillis(_), (TimeUtils.millisToZonedDateTime _).andThen(Right(_)))

  implicit val JobIdScalar =
    ScalarAlias[JobId, String](StringType, _.value, (JobId.apply _).andThen(Right(_)))

  implicit val RequestIdScalar =
    ScalarAlias[RequestId, String](StringType, _.value, (RequestId.apply _).andThen(Right(_)))

  implicit val OperatorEnumType = ProtoEnumType(Operator, "Operator")

  implicit val RequestType: ObjectType[GraphqlSecureContext, Request] = deriveObjectType[GraphqlSecureContext, Request](
    AddFields(
      Field(
        "childRequests",
        ListType(RequestType),
        resolve = ctx =>
          ctx.ctx.services.tx.trans
            .apply(
              DoobieRequestRepository.childRequest(ctx.value.requestId)
            )
            .unsafeToFuture()
      )
    )
  )

  implicit val JobType = deriveObjectType[GraphqlSecureContext, Job](
    AddFields(
      Field(
        "allRequests",
        ListType(RequestType),
        resolve = ctx =>
          ctx.ctx.services.tx.trans
            .apply(
              DoobieRequestRepository.jobRequests(ctx.value.id)
            )
            .unsafeToFuture()
      )
    )
  )

  implicit val TableRowType = deriveObjectType[GraphqlSecureContext, TableRow]()

  case class TableResult(count: Int, rows: List[TableRow])
  implicit val TableResultType = deriveObjectType[GraphqlSecureContext, TableResult]()

}
