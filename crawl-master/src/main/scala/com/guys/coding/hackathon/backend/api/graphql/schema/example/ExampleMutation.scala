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
import com.guys.coding.hackathon.backend.app.CrawlingService
import com.guys.coding.hackathon.backend.infrastructure.KafkaRequestService
import hero.common.util.IdProvider
import hero.common.util.time.TimeUtils.TimeProvider

class ExampleMutation(
    implicit exampleService: ExampleService[IO],
    idProvider: IdProvider[IO],
    timeProvider: TimeProvider[IO],
    kafkaRequestService: KafkaRequestService[IO]
) extends MutationHolder {

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
      ), {
        val phrasesArg  = Argument("phrases", ListInputType(StringType))
        val operatorArg = Argument("operator", StringType)
        Field(
          "startCrawling",
          StringType,
          arguments = phrasesArg :: operatorArg :: Nil,
          resolve = c => {
            CrawlingService
              .startFromPhrases[IO](c.arg(phrasesArg).toList, c.arg(operatorArg))
              .map(show => "...")
              .unsafeToFuture()
          }
        )
      }
    )
}
