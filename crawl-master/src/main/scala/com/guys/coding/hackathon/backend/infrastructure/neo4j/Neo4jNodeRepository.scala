package com.guys.coding.hackathon.backend.infrastructure.neo4j

import cats.effect.IO
import neotypes.Session
import neotypes.implicits.mappers.all._
import neotypes.implicits.syntax.all._

class Neo4jNodeRepository(session: Session[IO]) {

  def getPepole(): IO[List[(String, Int)]] = "match (p:Person) return p.name, p.born limit 10".query[(String, Int)].list(session)

}
