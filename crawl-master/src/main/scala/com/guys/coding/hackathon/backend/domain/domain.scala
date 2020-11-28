package com.guys.coding.hackathon.backend.domain

case class JobId(value: String)       extends AnyVal
case class RequestId(value: String)   extends AnyVal

case class EntityConfig(
    entityId: String,
    regex: String
)

object EntityConfig {
  val emailEntity: EntityConfig = EntityConfig(
    "email",
    """[^@ \t\r\n]+@[^@ \t\r\n]+\.[^@ \t\r\n]+"""
  )
  val phoneNumber: EntityConfig = EntityConfig(
    "phoneNumber",
    """^[\+]?[(]?[0-9]{3}[)]?[-\s\.]?[0-9]{3}[-\s\.]?[0-9]{4,6}$"""
  )
}

case class GlobalConfig(
    namedEntities: List[EntityConfig],
    discardedJobs: List[String]
)

case class EntityValue(value: String) extends AnyVal // email/phone/ query / etc
case class EntityId(value: String)    extends AnyVal // entity type
