package com.guys.coding.hackathon.backend.api.graphql.schema.example

import com.guys.coding.hackathon.backend.api.graphql.schema.QueryHolder
import com.guys.coding.hackathon.backend.api.graphql.service.GraphqlSecureContext
import hero.common.sangria.pagination.{PaginationArgs, PaginationTypes}
import sangria.schema._
import com.guys.coding.hackathon.backend.domain.ExampleService
import cats.effect.IO
import com.guys.coding.hackathon.backend.infrastructure.postgres.DoobieExampleRepository
import doobie.util.transactor.Transactor

class ExampleQuery(exampleService: ExampleService[IO], tx: Transactor[IO]) extends QueryHolder {

  import ExampleTypes.UserType
  import PaginationArgs._
  import PaginationTypes._

  override def queryFields(): List[Field[GraphqlSecureContext, Unit]] =
    fields[GraphqlSecureContext, Unit](
      Field(
        "bestShowsEver",
        PaginatedType(
          ofType = NodeType[String](StringType, name = "Example"),
          "Example"
        ),
        arguments = PageArg :: EntriesPerPageArg :: Nil,
        resolve = ctx => {
          // ctx.ctx.authorizedF { _ =>
          val entriesPerPage = ctx.arg(EntriesPerPageArg)

          exampleService.getBestShowEver
            .map(show =>
              Paginated(
                entities = (1 to entriesPerPage).map(_ => Node(show)).toList,
                3
              )
            )
            .unsafeToFuture()
        }
      ),
      Field(
        "backendUsers",
        ListType(UserType),
        arguments = Nil,
        resolve = ctx => // ctx.ctx.authorizedF { _ =>
        {
          tx.trans
            .apply(
              DoobieExampleRepository.getUsers()
            )
            .unsafeToFuture()
        }
      )
    )
}
