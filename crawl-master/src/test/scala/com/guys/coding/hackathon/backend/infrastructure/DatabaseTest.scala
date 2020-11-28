package com.guys.coding.hackathon.backend.infrastructure

import _root_.doobie.scalatest.IOChecker
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieExampleRepository.Statements
import org.scalatest.flatspec.AnyFlatSpec
import java.time.ZonedDateTime

class DatabaseTest extends AnyFlatSpec with IOChecker with PostgresSpec {

  "ExampleRepository" should "have correct createTable" in {
    check(Statements.createTable)
  }

  it should "have correct update" in {
    check(Statements.insertUser("id", "name", None, ZonedDateTime.now()))
  }

  it should "have correct query" in {
    check(Statements.getUsers)
  }

}
