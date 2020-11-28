package com.guys.coding.hackathon.backend

import com.guys.coding.hackathon.backend.domain.{ExampleService, TokenService}
import cats.effect.IO
import com.guys.coding.hackathon.backend.infrastructure.KafkaRequestService
import doobie.util.transactor.Transactor

case class Services(
    exampleService: ExampleService[IO],
    jwtTokenService: TokenService,
    tx: Transactor[IO],
    kafkaRequestService: KafkaRequestService[IO]
)
