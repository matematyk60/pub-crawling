package com.guys.coding.hackathon.backend.infrastructure.kafka

import cats.Applicative
import cats.effect.{ConcurrentEffect, Sync}
import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.functor.toFunctorOps
import com.guys.coding.hackathon.proto.notifcation.{Crawl, Request}
import fs2.kafka._

class KafkaRequestService[F[_]: ConcurrentEffect](topic: String, producer: KafkaProducer[F, String, Request]) {
  def sendRequests(requests: List[Request]): F[Unit] = {
    val records = requests.map { request =>
      val partitionKey = request.is match {
        case Request.Is.Crawl(Crawl(requestId, _, _, _, _)) =>
          requestId
        case _ => throw new IllegalStateException()
      }
      ProducerRecord(topic, partitionKey, request)
    }
    producer.produce(ProducerRecords.apply[List, String, Request](records)).flatten.void
  }
}

object KafkaRequestService {

  def apply[F[_]](implicit k: KafkaRequestService[F]): KafkaRequestService[F] = k

  def producerSettings[F[_]: ConcurrentEffect](bootstrapServers: String): ProducerSettings[F, String, Request] =
    ProducerSettings
      .apply[F, String, Request](
        Serializer.string[F],
        serializer
      )
      .withBootstrapServers(bootstrapServers)
      .withAcks(Acks.All)

  def serializer[F[_]: Sync]: Serializer[F, Request] =
    Serializer.lift(message => Applicative[F].pure(message.toByteArray))
}
