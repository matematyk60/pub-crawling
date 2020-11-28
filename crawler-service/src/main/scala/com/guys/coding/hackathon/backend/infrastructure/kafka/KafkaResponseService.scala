package com.guys.coding.hackathon.backend.infrastructure.kafka

import cats.effect.ContextShift
import fs2.kafka._
import com.guys.coding.hackathon.backend.domain.ResponseService
import cats.effect.IO
import com.guys.coding.hackathon.proto.notifcation._
import cats.syntax.flatMap.toFlatMapOps
import com.guys.coding.hackathon.proto.notifcation.Response.Is.Empty

class KafkaResponseService(topic: String, producer: KafkaProducer[IO, String, Array[Byte]])(implicit cs: ContextShift[IO])
    extends ResponseService[IO] {

  override def send(response: Response): IO[Unit] = {
    val record = ProducerRecords.one(responseProducerRecord(response))
    producer.produce(record).flatten.as(())
  }

  private def responseProducerRecord(response: Response) =
    ProducerRecord(topic, key = responseKey(response), value = response.toByteArray)

  private def responseKey(response: Response): String =
    response.is match {
      case Empty                        => throw new IllegalStateException("Empty")
      case Response.Is.Success(success) => success.requestId
      case Response.Is.Failure(failure) => failure.requestId
    }
}

object KafkaResponseService {
  def producerSettings(bootstrapServers: String): ProducerSettings[IO, String, Array[Byte]] =
    ProducerSettings
      .apply[IO, String, Array[Byte]](
        Serializer.string[IO],
        Serializer.identity[IO]
      )
      .withBootstrapServers(bootstrapServers)
      .withAcks(Acks.All)
}
