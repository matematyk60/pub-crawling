package com.guys.coding.hackathon.backend.api.graphql.schema.example

import sangria.macros.derive._
import sangria.schema._
import com.guys.coding.hackathon.backend.api.graphql.service.GraphqlSecureContext
import com.guys.coding.hackathon.backend.domain.{JobId, RequestId}
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieJobRepository.Job
import hero.common.util.time.TimeUtils
import java.time.ZonedDateTime

object ExampleTypes {
  case class ExampleInput(id: String, value: String)
  implicit val ExampleInputType: InputType[ExampleInput] =
    deriveInputObjectType[ExampleInput]()

  implicit val ZonedScalar =
    ScalarAlias[ZonedDateTime, Long](LongType, TimeUtils.zonedDateTimeToMillis(_), (TimeUtils.millisToZonedDateTime _).andThen(Right(_)))

  implicit val JobIdScalar =
    ScalarAlias[JobId, String](StringType, _.value, (JobId.apply _).andThen(Right(_)))

  implicit val RequestIdScalar =
    ScalarAlias[RequestId, String](StringType, _.value, (RequestId.apply _).andThen(Right(_)))

  implicit val UserType = deriveObjectType[GraphqlSecureContext, Job]()

}
