package com.guys.coding.hackathon.backend.domain

import com.guys.coding.hackathon.backend.{AdminId, Token}
import com.guys.coding.hackathon.backend.domain.admin.AdminTokenPayload

import scala.util.Try

trait TokenService {

  def validateToken(token: String): Try[AdminTokenPayload]

  def generateToken(admin: AdminId): Token
}
