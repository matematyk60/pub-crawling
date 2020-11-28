package com.guys.coding.hackathon.backend.infrastructure

import _root_.doobie.scalatest.IOChecker
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieJobRepository.{Statements => JobS}
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieRequestRepository.{Statements => RequestS}
import org.scalatest.flatspec.AnyFlatSpec
import java.time.ZonedDateTime
import com.guys.coding.hackathon.proto.notifcation.Query.Operator
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieRequestRepository.Request
import com.guys.coding.hackathon.backend.domain.RequestId
import com.guys.coding.hackathon.backend.domain.JobId

class DatabaseTest extends AnyFlatSpec with IOChecker with PostgresSpec {

  "DoobieJobRepository" should "have correct createTable" in {
    check(JobS.createTable)
  }

  it should "have correct update" in {
    check(JobS.insertUser("id", "name", RequestId("rid"), ZonedDateTime.now()))
  }

  it should "have correct query" in {
    check(JobS.getJobs)
  }

  "DoobieRequestRepository" should "have correct createTable" in {
    check(RequestS.createTable)
  }

  it should "have correct update" in {
    check(RequestS.insertRequest(Request(RequestId("da"), None, JobId("dad"), Operator.AND, List("ala", "makota"), ZonedDateTime.now())))
    check(RequestS.insertRequest(Request(RequestId("da"), Some(RequestId("ola")), JobId("dad"), Operator.OR, List.empty, ZonedDateTime.now())))
  }

  it should "have correct query" in {
    check(RequestS.jobsRequests(JobId("ala")))
  }

}
