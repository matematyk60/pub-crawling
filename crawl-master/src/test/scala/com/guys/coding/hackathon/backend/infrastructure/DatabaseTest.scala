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
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieJobRepository.Job

class DatabaseTest extends AnyFlatSpec with IOChecker with PostgresSpec {

  "DoobieJobRepository" should "have correct createTable" in {
    check(JobS.createTable)
  }

  it should "have correct update" in {
    check(
      JobS.insertUser(
        Job(
          JobId("id"),
          parentJob = Some(JobId("ala")),
          jobDepth = 1,
          name = "dala",
          startTime = ZonedDateTime.now(),
          operator = Operator.AND,
          phrases = List("ala", "makota"),
          iterations = 1
        )
      )
    )

    check(
      JobS.insertUser(
        Job(
          JobId("id"),
          parentJob = None,
          jobDepth = 1,
          name = "dala",
          startTime = ZonedDateTime.now(),
          operator = Operator.OR,
          phrases = Nil,
          iterations = 1
        )
      )
    )
  }

  it should "have correct query" in {
    check(JobS.getJobs)
  }

  "DoobieRequestRepository" should "have correct createTable" in {
    check(RequestS.createTable)
  }

  it should "have correct update" in {
    check(RequestS.insertRequest(Request(RequestId("da"), "url", None, depth = 0, JobId("dad"), ZonedDateTime.now())))
    check(RequestS.insertRequest(Request(RequestId("da"), "url", Some(RequestId("ola")), depth = 1, JobId("dad"), ZonedDateTime.now())))
  }

  it should "have correct query" in {
    check(RequestS.jobsRequests(JobId("ala")))
    check(RequestS.childRequests(RequestId("ala")))
    check(RequestS.get(RequestId("ala")))
  }

  it should "set request complete" in {
    check(RequestS.setRequestComplete(RequestId("lana"), success = true).update)
  }

}
