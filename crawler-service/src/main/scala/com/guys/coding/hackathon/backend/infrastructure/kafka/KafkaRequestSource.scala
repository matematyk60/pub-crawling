package com.guys.coding.hackathon.backend.infrastructure.kafka

import cats.effect.{ContextShift, IO, Timer}
import fs2.Stream
import fs2.kafka.{consumerStream, AutoOffsetReset, ConsumerSettings, Deserializer}
import hero.common.logging.{Logger, LoggingSupport}
import com.guys.coding.hackathon.proto.notifcation.Request

import scala.util.Try

class KafkaRequestSource(groupId: String, clientId: String, bootstrapServers: String, topic: String)(
    implicit
    cs: ContextShift[IO],
    timer: Timer[IO],
    logger: Logger[IO]
) extends LoggingSupport
    with KafkaSource[IO, Request] {

  private val settings =
    ConsumerSettings
      .apply(
        Deserializer.identity[IO],
        Deserializer.identity[IO]
      )
      .withBootstrapServers(bootstrapServers)
      .withClientId(clientId)
      .withGroupId(groupId)
      .withAutoOffsetReset(AutoOffsetReset.Latest)

  def source: Stream[IO, Stream[IO, Committable[IO, Request]]] =
    consumerStream[IO]
      .using(settings)
      .evalTap(_.subscribeTo(topic))
      .flatMap(_.partitionedStream)
      .map(
        _.evalMap { committable =>
          Try(Committable(Request.parseFrom(committable.record.value), committable.offset)).toEither match {
            case Left(ex) =>
              logger
                .error(s"Error while parsing value of record ${committable.offset}. Omitting...", ex)
                .as(None)

            case Right(request) =>
              IO.pure(Some(request))
          }
        }.flatMap(maybeRequest => Stream.emits(maybeRequest.toList))
      )

}
