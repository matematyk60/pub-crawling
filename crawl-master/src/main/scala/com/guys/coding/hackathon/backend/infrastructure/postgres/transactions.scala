package com.guys.coding.hackathon.backend.infrastructure.postgres

import cats.effect.IO
import cats.~>
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor

object transactions {
  implicit class TransactionOps[A, DB[_], E[_]](dbio: DB[A])(implicit tx: DB ~> E) {
    def transact: E[A] = tx(dbio)
  }

  def doobieTransactorTransformer(xa: Transactor[IO]): ConnectionIO ~> IO =
    new ~>[ConnectionIO, IO] {
      override def apply[A](dbio: ConnectionIO[A]): IO[A] = dbio.transact(xa)
    }

}
