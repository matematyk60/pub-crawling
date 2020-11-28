package com.guys.coding.hackathon.backend.api.graphql.schema

import com.guys.coding.hackathon.backend.api.graphql.service.GraphqlSecureContext
import sangria.schema.Field

trait MutationHolder {
  def mutationFields(): List[Field[GraphqlSecureContext, Unit]]
}
