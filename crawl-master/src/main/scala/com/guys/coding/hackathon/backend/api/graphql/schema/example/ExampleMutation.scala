package com.guys.coding.hackathon.backend.api.graphql.schema.example

import cats.effect.IO
import com.guys.coding.hackathon.backend.api.graphql.schema.MutationHolder
import com.guys.coding.hackathon.backend.api.graphql.service.GraphqlSecureContext
import com.guys.coding.hackathon.backend.app.CrawlingService
import com.guys.coding.hackathon.backend.domain.EntityService
import com.guys.coding.hackathon.backend.domain.ExampleService
import com.guys.coding.hackathon.backend.domain.JobId
import com.guys.coding.hackathon.backend.infrastructure.kafka.KafkaRequestService
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieJobRepository
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieRequestRepository
import com.guys.coding.hackathon.backend.infrastructure.redis.RedisConfigRepository
import hero.common.util.IdProvider
import hero.common.util.time.TimeUtils.TimeProvider
import io.circe.generic.auto._
import sangria.marshalling.circe._
import sangria.schema._

class ExampleMutation(
    implicit exampleService: ExampleService[IO],
    idProvider: IdProvider[IO],
    timeProvider: TimeProvider[IO],
    kafkaRequestService: KafkaRequestService[IO],
    dJ: DoobieJobRepository[IO],
    dRR: DoobieRequestRepository[IO],
    rcR: RedisConfigRepository[IO],
    entityService: EntityService[IO]
) extends MutationHolder {

  import ExampleTypes._

  val ExampleArg = Argument("example", ExampleInputType)

  override def mutationFields(): List[Field[GraphqlSecureContext, Unit]] =
    fields[GraphqlSecureContext, Unit](
      {
        val phrasesArg               = Argument("phrases", ListInputType(StringType))
        val operatorArg              = Argument("operator", StringType)
        val iterationsArg            = Argument("iterations", IntType)
        val emailEntityEnabled       = Argument("emailEntityEnabled", BooleanType)
        val phoneNumberEntityEnabled = Argument("phoneNumberEntityEnabled", BooleanType)
        val bitcoinAddressEnabled    = Argument("bitcoinAddressEnabled", BooleanType)
        val ssnNumberEnabled         = Argument("ssnNumberEnabled", BooleanType)
        val creditCardEnabled        = Argument("creditCardEnabled", BooleanType)
        val selectedDomains          = Argument("selectedDomains", ListInputType(StringType))

        Field(
          "startCrawling",
          StringType,
          arguments = phrasesArg :: operatorArg :: iterationsArg :: emailEntityEnabled :: phoneNumberEntityEnabled ::
            bitcoinAddressEnabled ::
            ssnNumberEnabled ::
            creditCardEnabled ::
          selectedDomains ::
            Nil,
          resolve = c => {
            CrawlingService
              .startFromPhrases[IO](
                phrases = c.arg(phrasesArg).toList,
                operator = c.arg(operatorArg),
                jobIterations = c.arg(iterationsArg),
                emailEntityEnabled = c.arg(emailEntityEnabled),
                phoneNumberEntityEnabled = c.arg(phoneNumberEntityEnabled),
                bitcoinAddressEnabled = c.arg(bitcoinAddressEnabled),
                ssnNumberEnabled = c.arg(ssnNumberEnabled),
                creditCardEnabled = c.arg(creditCardEnabled),
                selectedDomains = c.arg(selectedDomains).toList
              )
              .map(_.value)
              .unsafeToFuture()
          }
        )
      }, {
        val ParentJobId              = Argument("parentJobId", StringType)
        val EntityValuesArg          = Argument("entityValues", ListInputType(StringType))
        val iterationsArg            = Argument("iterations", IntType)
        val emailEntityEnabled       = Argument("emailEntityEnabled", BooleanType)
        val phoneNumberEntityEnabled = Argument("phoneNumberEntityEnabled", BooleanType)
        val bitcoinAddressEnabled    = Argument("bitcoinAddressEnabled", BooleanType)
        val ssnNumberEnabled         = Argument("ssnNumberEnabled", BooleanType)
        val creditCardEnabled        = Argument("creditCardEnabled", BooleanType)
        val selectedDomains          = Argument("selectedDomains", ListInputType(StringType))

        Field(
          "crawlChoosenEntities",
          ListType(StringType),
          arguments = ParentJobId :: EntityValuesArg :: iterationsArg :: emailEntityEnabled :: phoneNumberEntityEnabled ::
            bitcoinAddressEnabled ::
            ssnNumberEnabled ::
            creditCardEnabled ::
            Nil,
          resolve = c => {
            CrawlingService
              .crawlFromEntities[IO](
                jobId = JobId(c.arg(ParentJobId)),
                choosenEntityValues = c.arg(EntityValuesArg).toList,
                jobIterations = c.arg(iterationsArg),
                emailEntityEnabled = c.arg(emailEntityEnabled),
                phoneNumberEntityEnabled = c.arg(phoneNumberEntityEnabled),
                bitcoinAddressEnabled = c.arg(bitcoinAddressEnabled),
                ssnNumberEnabled = c.arg(ssnNumberEnabled),
                creditCardEnabled = c.arg(creditCardEnabled),
                selectedDomains = c.arg(selectedDomains).toList
              )
              .map(r => r.map(_.value))
              .unsafeToFuture()
          }
        )
      }, {
        val jobId = Argument("jobId", StringType)

        Field(
          "stopCrawling",
          StringType,
          arguments = jobId :: Nil,
          resolve = c => {
            CrawlingService.cancelJob[IO](JobId(c.arg(jobId))).map(_ => c.arg(jobId)).unsafeToFuture()
          }
        )
      }
    )
}
