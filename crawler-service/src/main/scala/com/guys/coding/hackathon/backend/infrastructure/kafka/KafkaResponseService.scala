package com.guys.coding.hackathon.backend.infrastructure.kafka

import cats.effect.ContextShift
import fs2.kafka._
import com.guys.coding.hackathon.backend.domain.ResponseService
import cats.effect.IO
import com.guys.coding.hackathon.proto.notifcation._
import cats.syntax.flatMap.toFlatMapOps
import com.guys.coding.hackathon.proto.notifcation.Request.Is.Empty

class KafkaResponseService(topic: String, producer: KafkaProducer[IO, String, Array[Byte]])(implicit cs: ContextShift[IO])
    extends ResponseService[IO] {

  override def send(request: Request): IO[Unit] = {
    val record = ProducerRecords.one(requestProducerRecord(request))
    producer.produce(record).flatten.as(())
  }

  private def requestProducerRecord(request: Request) =
    ProducerRecord(topic, key = requestKey(request), value = request.toByteArray)

  private def requestKey(request: Request): String =
    request.is match {
      case Empty                   => throw new IllegalStateException("Empty")
      case Request.Is.Crawl(crawl) => crawl.requestId
    }
}
