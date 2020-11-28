package com.guys.coding.hackathon.backend.api.graphql

import com.guys.coding.hackathon.backend.Services
import com.guys.coding.hackathon.backend.api.graphql.schema.QueryHolder
import com.guys.coding.hackathon.backend.api.graphql.schema.example.JobQuery
import sangria.schema.ObjectType

class Query(services: Services) {

  private val queryHolders = List[QueryHolder](
    new JobQuery(services.tx)
  )

  val QueryType = ObjectType(
    name = "Query",
    fields = queryHolders.flatMap(_.queryFields())
  )
}
