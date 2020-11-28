package com.guys.coding.hackathon.backend.api.graphql.schema.example

import com.guys.coding.hackathon.backend.api.graphql.schema.MutationHolder
import com.guys.coding.hackathon.backend.api.graphql.service.GraphqlSecureContext
import hero.common.sangria.mutation.MutationResultType
import hero.common.sangria.mutation.MutationResultType.MutationResult
import sangria.schema._
import io.circe.generic.auto._
import sangria.marshalling.circe._
import com.guys.coding.hackathon.backend.domain.ExampleService
import cats.effect.IO

class ExampleMutation(exampleService: ExampleService[IO]) extends MutationHolder {

  import ExampleTypes._

  val ExampleArg = Argument("example", ExampleInputType)

  override def mutationFields(): List[Field[GraphqlSecureContext, Unit]] =
    fields[GraphqlSecureContext, Unit](
      Field(
        "addExample",
        MutationResultType(StringType, StringType, "CreateExampleResult"),
        arguments = ExampleArg :: Nil,
        resolve = _ => {
          exampleService.getBestShowEver
            .map(show => MutationResult[String, String](None, Some(show)))
            .unsafeToFuture()

        }
      )
    )
}
