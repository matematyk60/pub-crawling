package com.guys.coding.hackathon.backend.infrastructure.jwt

import hero.common.util.time.TimeUtils.Implicits._millisToZonedDateTime
// import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

import java.security.{PrivateKey, PublicKey}
import java.time.ZonedDateTime
import java.util.UUID

import com.guys.coding.hackathon.backend.{AdminId, Token}

import scala.concurrent.duration._
import com.guys.coding.hackathon.backend.domain.TokenService
import com.guys.coding.hackathon.backend.domain.admin.AdminTokenPayload
import scala.language.postfixOps
import scala.util.Try
import pdi.jwt.JwtAlgorithm
import pdi.jwt.JwtCirce
import pdi.jwt.JwtClaim

class JwtTokenService(publicKey: PublicKey, privateKey: PrivateKey) extends TokenService {

  private val algorithm      = JwtAlgorithm.RS256
  private val TOKEN_DURATION = 30 days
  private val ISSUER         = "hackathon-backend"
  private val AUDIENCE       = "hackathon-backend"

  def generateToken(adminId: AdminId): Token = {
    val issuedTime = ZonedDateTime.now.toEpochSecond
    val expiration =
      ZonedDateTime.now.plusSeconds(TOKEN_DURATION.toSeconds).toEpochSecond
    val claims = JwtClaim(
      issuer = Some(ISSUER),
      audience = Some(Set(AUDIENCE)),
      subject = Some(adminId.value),
      expiration = Some(expiration),
      issuedAt = Some(issuedTime),
      jwtId = Some(UUID.randomUUID().toString.filter(_ != '-'))
    )
    Token(JwtCirce.encode(claims, privateKey, algorithm))
  }

  def validateToken(accessToken: String): Try[AdminTokenPayload] = {

    JwtCirce.decode(accessToken, publicKey, Seq(algorithm)).map { claim =>
      if (!claim.issuer.forall(_ == ISSUER))
        throw new IllegalStateException(s"Invalid issuer [${claim.issuer}]")
      if (!claim.audience.forall(_.contains(AUDIENCE)))
        throw new IllegalStateException(
          s"Invalid audience [${claim.audience}]"
        )
      if (claim.issuedAt.isEmpty)
        throw new IllegalStateException(s"IssuedAt must be defined")

      val adminId = claim.subject.getOrElse(
        throw new IllegalStateException(s"Invalid subject [${claim.subject}]")
      )

      AdminTokenPayload(AdminId(adminId), claim.issuedAt.get)
    }
  }
}
