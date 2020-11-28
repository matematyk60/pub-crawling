package com.guys.coding.hackathon.backend.api.graphql.service

import com.typesafe.scalalogging.StrictLogging
import com.guys.coding.hackathon.backend.Token
import com.guys.coding.hackathon.backend.domain.TokenService
import com.guys.coding.hackathon.backend.domain.admin.AuthenticatedAdmin

import scala.concurrent.{ExecutionContext, Future}

case class Authenticator(token: Option[Token], tokenService: TokenService)(
    implicit ec: ExecutionContext
) extends StrictLogging {

  def authorized: Future[AuthenticatedAdmin] = token match {
    case Some(tokenValue) =>
      payloadFromToken(tokenValue).map(payload => AuthenticatedAdmin(payload.adminId))
    case None =>
      Future.failed(AuthenticationException)
  }

  private def payloadFromToken(token: Token) =
    Future.fromTry(tokenService.validateToken(token.value)).recover {
      case ex: Throwable =>
        logger.warn(s"Error while authenticating an admin", ex)
        throw AuthenticationException
    }
}

case object AuthenticationException extends Throwable
