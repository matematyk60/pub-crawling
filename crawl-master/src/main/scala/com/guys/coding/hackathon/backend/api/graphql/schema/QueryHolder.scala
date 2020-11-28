package com.guys.coding.hackathon.backend.api.graphql.schema

import com.guys.coding.hackathon.backend.api.graphql.service.GraphqlSecureContext
import sangria.schema.Field

trait QueryHolder {
  def queryFields(): List[Field[GraphqlSecureContext, Unit]]
}
