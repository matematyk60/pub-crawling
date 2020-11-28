package com.guys.coding.hackathon.backend.domain.admin

import java.time.ZonedDateTime

import com.guys.coding.hackathon.backend.AdminId

case class AuthenticatedAdmin(adminId: AdminId)

case class AdminTokenPayload(adminId: AdminId, issuedAt: ZonedDateTime)
