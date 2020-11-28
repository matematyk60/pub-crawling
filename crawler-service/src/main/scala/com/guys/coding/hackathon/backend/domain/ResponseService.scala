package com.guys.coding.hackathon.backend.domain

import com.guys.coding.hackathon.proto.notifcation.Response
import simulacrum.typeclass

@typeclass
trait ResponseService[F[_]] {
  def send(response: Response): F[Unit]
}
