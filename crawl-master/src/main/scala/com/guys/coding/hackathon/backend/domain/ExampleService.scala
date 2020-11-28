package com.guys.coding.hackathon.backend.domain

import cats.Monad
import simulacrum.typeclass

@typeclass trait ExampleService[F[_]] {
  def getBestShowEver(implicit m: Monad[F]): F[String] =
    Monad[F].pure("La Casa de Papel")
}
