package com.guys.coding.hackathon.backend.api.graphql.core

import cats.syntax.applicativeError._
import com.guys.coding.hackathon.backend.api.graphql.{Mutation, Query}
import com.typesafe.scalalogging.StrictLogging
import com.guys.coding.hackathon.backend.{Services, Token}
import com.guys.coding.hackathon.backend.api.graphql.service.{AuthenticationException, GraphqlSecureContext}
import io.circe.Json
import io.circe.optics.JsonPath._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.util.CaseInsensitiveString
import org.http4s.HttpRoutes
import sangria.execution._
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import sangria.schema.Schema
import cats.data.NonEmptyList
import org.http4s.headers.`WWW-Authenticate`
import org.http4s.Challenge
import cats.effect.{ContextShift, IO}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class GraphqlRoute(services: Services)(implicit ec: ExecutionContext, cs: ContextShift[IO]) extends StrictLogging {

  private val authenticationErrorKey = "special-authentication-error-key"
  private val handler = ExceptionHandler {
    case (_, AuthenticationException) =>
      SingleHandledException(authenticationErrorKey)
  }
  private val schema = Schema(
    new Query(services).QueryType,
    Some(new Mutation(services).MutationType)
  )

  def route: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case request @ POST -> Root =>
      request.as[Json].flatMap { body =>
        val bearerToken = request.headers
          .get(CaseInsensitiveString("Authorization"))
          .map(_.value.stripPrefix("Bearer "))
        val query     = root.query.string.getOption(body)
        val operation = root.operationName.string.getOption(body)
        val vars      = root.variables.json.getOption(body) getOrElse Json.obj()
        val context =
          GraphqlSecureContext(bearerToken.map(Token), services.jwtTokenService)

        query.map(QueryParser.parse(_)) match {
          case Some(Success(value)) =>
            IO.fromFuture(
                IO(
                  Executor
                    .execute(
                      schema,
                      value,
                      context,
                      variables = vars,
                      operationName = operation,
                      exceptionHandler = handler
                    )
                )
              )
              .flatMap {
                case response
                    if response.hcursor
                      .downField("errors")
                      .get[List[String]]("message")
                      .exists(_.contains(authenticationErrorKey)) =>
                  Unauthorized(
                    `WWW-Authenticate`(
                      NonEmptyList.of(
                        Challenge(scheme = "Bearer", realm = "Token invalid")
                      )
                    )
                  )
                case response => Ok(response)
              }
              .recoverWith {
                case error: QueryAnalysisError =>
                  BadRequest(error.resolveError)
                case error: ErrorWithResolver =>
                  InternalServerError(error.resolveError)
                case other =>
                  logger
                    .error("Request completed with InternalSererError", other)
                  InternalServerError()
              }
          case Some(Failure(error)) =>
            BadRequest(Json.obj("error" -> Json.fromString(error.getMessage)))
          case None =>
            BadRequest(
              Json.obj("error" -> Json.fromString("No query to execute"))
            )
        }
      }
  }
}
