package com.guys.coding.hackathon.backend.domain

import com.guys.coding.hackathon.proto.notifcation.Request
import simulacrum.typeclass

@typeclass
trait ResponseService[F[_]] {
  def send(request: Request): F[Unit]
}
