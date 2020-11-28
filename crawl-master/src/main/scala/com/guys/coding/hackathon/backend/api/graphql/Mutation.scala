package com.guys.coding.hackathon.backend.api.graphql

import java.time.Clock

import com.guys.coding.hackathon.backend.Services
import com.guys.coding.hackathon.backend.api.graphql.schema.MutationHolder
import com.guys.coding.hackathon.backend.api.graphql.schema.example.ExampleMutation
import hero.common.util.IdProvider
import hero.common.util.time.TimeUtils.TimeProvider
import sangria.schema.ObjectType

class Mutation(services: Services) {

  private val mutationHolders =
    List[MutationHolder](
      new ExampleMutation()(
        services.exampleService,
        IdProvider.io,
        TimeProvider.io(Clock.systemUTC()),
        services.kafkaRequestService,
        services.dJ,
        services.dRR,
        services.rcR,
        services.enttityService
      )
    )

  val MutationType = ObjectType(
    name = "Mutation",
    fields = mutationHolders.flatMap(_.mutationFields())
  )
}
