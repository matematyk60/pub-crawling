package com.guys.coding.hackathon.backend.infrastructure.neo4j

import cats.effect.IO
import neotypes.Session
import neotypes.implicits.mappers.all._
import neotypes.implicits.syntax.all._

class Neo4jNodeRepository(session: Session[IO]) {

  // https://neotypes.github.io/neotypes/
  // NOTE: neotypes.generic is scheduled for 0.16.0, now use  neotypes.implicits.mappers.all._ instead.

  def getPepole(): IO[List[(String, Int)]] = "match (p:Person) return p.name, p.born limit 10".query[(String, Int)].list(session)

}
