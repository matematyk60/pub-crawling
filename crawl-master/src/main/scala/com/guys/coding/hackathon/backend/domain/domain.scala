package com.guys.coding.hackathon.backend.domain

case class JobId(value: String)     extends AnyVal
case class RequestId(value: String) extends AnyVal

case class EntityConfig(
    entityId: String,
    regex: String
)

object EntityConfig {
  val emailEntity: EntityConfig = EntityConfig(
    "email",
    """(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])"""
  )
  val phoneNumber: EntityConfig = EntityConfig(
    "phoneNumber",
    """^[\+]?[(]?[0-9]{3}[)]?[-\s\.]?[0-9]{3}[-\s\.]?[0-9]{4,6}$"""
  )

  val bitcoinAddress: EntityConfig = EntityConfig(
    "bitcoinAddress",
    """^(bc1|[13])[a-zA-HJ-NP-Z0-9]{25,39}$"""
  )

  val ssnNumber: EntityConfig = EntityConfig(
    "ssnNumber",
    """^(?!0{3})(?!6{3})[0-8]\d{2}-(?!0{2})\d{2}-(?!0{4})\d{4}$"""
  )

  val creditCard: EntityConfig = EntityConfig(
    "creditCard",
    """(^4[0-9]{12}(?:[0-9]{3})?$)|(^(?:5[1-5][0-9]{2}|222[1-9]|22[3-9][0-9]|2[3-6][0-9]{2}|27[01][0-9]|2720)[0-9]{12}$)|(3[47][0-9]{13})|(^3(?:0[0-5]|[68][0-9])[0-9]{11}$)|(^6(?:011|5[0-9]{2})[0-9]{12}$)|(^(?:2131|1800|35\d{3})\d{11}$)"""
  )
}

case class GlobalConfig(
    namedEntities: List[EntityConfig],
    discardedJobs: List[String]
)

case class EntityValue(value: String) extends AnyVal // email/phone/ query / etc
case class EntityId(value: String)    extends AnyVal // entity type
