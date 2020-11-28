package com.guys.coding.hackathon.backend

import com.guys.coding.hackathon.backend.domain.{ExampleService, TokenService}
import cats.effect.IO
import doobie.util.transactor.Transactor

case class Services(exampleService: ExampleService[IO], jwtTokenService: TokenService, tx: Transactor[IO])
