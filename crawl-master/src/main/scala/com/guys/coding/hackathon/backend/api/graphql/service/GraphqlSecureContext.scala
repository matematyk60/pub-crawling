package com.guys.coding.hackathon.backend.api.graphql.service

import com.typesafe.scalalogging.StrictLogging
import com.guys.coding.hackathon.backend.Token
import com.guys.coding.hackathon.backend.domain.TokenService
import com.guys.coding.hackathon.backend.domain.admin.AuthenticatedAdmin

import scala.concurrent.{ExecutionContext, Future}

case class GraphqlSecureContext(
    token: Option[Token],
    tokenService: TokenService
)(implicit ec: ExecutionContext)
    extends StrictLogging {

  private val authenticator = Authenticator(token, tokenService)

  def authorized[T](fn: AuthenticatedAdmin => T): Future[T] =
    authorizedF(Future.successful[T] _ compose fn)

  def authorizedF[T](fn: AuthenticatedAdmin => Future[T]): Future[T] =
    authenticator.authorized.flatMap(fn)
}
