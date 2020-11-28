package com.guys.coding.hackathon.backend.domain

case class CrawlingConfig(
    namedEntities: List[NamedEntity]
)

case class NamedEntity(
    entityId: String,
    regex: String
)
