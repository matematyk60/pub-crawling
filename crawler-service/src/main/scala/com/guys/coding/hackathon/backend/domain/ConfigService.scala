package com.guys.coding.hackathon.backend.domain

import simulacrum.typeclass

@typeclass
trait ConfigService[F[_]] {
  def get(): F[Option[CrawlingConfig]]
}
