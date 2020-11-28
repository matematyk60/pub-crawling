package com.guys.coding.hackathon.backend.domain

case class CrawlingConfig(
    namedEntities: List[NamedEntity],
    discardedJobs: Set[String]
)

case class NamedEntity(
    entityId: String,
    regex: String
)
