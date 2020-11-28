package com.guys.coding.hackathon.backend

import com.guys.coding.hackathon.backend.domain.{ExampleService, TokenService}
import cats.effect.IO
import com.guys.coding.hackathon.backend.infrastructure.kafka.KafkaRequestService
import com.guys.coding.hackathon.backend.infrastructure.postgres.{DoobieJobRepository, DoobieRequestRepository}
import com.guys.coding.hackathon.backend.infrastructure.redis.RedisConfigRepository
import doobie.util.transactor.Transactor

case class Services(
    exampleService: ExampleService[IO],
    jwtTokenService: TokenService,
    tx: Transactor[IO],
    kafkaRequestService: KafkaRequestService[IO],
    dJ: DoobieJobRepository[IO],
    dRR: DoobieRequestRepository[IO],
    rcR: RedisConfigRepository[IO]
)
