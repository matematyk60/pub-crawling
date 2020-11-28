package com.guys.coding.hackathon.backend.infrastructure.kafka

import fs2.Stream
import fs2.kafka.CommittableOffset

trait KafkaSource[F[_], T] {
  def source: Stream[F, Stream[F, Committable[F, T]]]
}

object KafkaSource {
  def apply[F[_], T](implicit ks: KafkaSource[F, T]) = ks
}

case class Committable[F[_], T](value: T, offset: CommittableOffset[F])

