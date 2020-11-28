package com.guys.coding.hackathon.backend.infrastructure.kafka

import cats.effect.{ConcurrentEffect, ContextShift, Sync, Timer}
import com.guys.coding.hackathon.backend.infrastructure.kafka.KafkaResponseSource.deserializer
import com.guys.coding.hackathon.proto.notifcation.Response
import fs2.Stream
import fs2.kafka._

class KafkaResponseSource[F[_]: ConcurrentEffect](groupId: String, clientId: String, bootstrapServers: String, topic: String)(
    implicit cs: ContextShift[F],
    timer: Timer[F]
) {

  private val settings =
    ConsumerSettings
      .apply(
        Deserializer.string[F],
        deserializer
      )
      .withBootstrapServers(bootstrapServers)
      .withClientId(clientId)
      .withGroupId(groupId)
      .withAutoOffsetReset(AutoOffsetReset.Latest)

  def source: Stream[F, CommittableConsumerRecord[F, String, Response]] =
    consumerStream[F]
      .using(settings)
      .evalTap(_.subscribeTo(topic))
      .flatMap(_.stream)

}

object KafkaResponseSource {

  def apply[F[_]](implicit k: KafkaResponseSource[F]): KafkaResponseSource[F] = k

  def deserializer[F[_]: Sync]: Deserializer[F, Response] =
    Deserializer.lift(bytes => Sync[F].delay(Response.parseFrom(bytes)))
}


