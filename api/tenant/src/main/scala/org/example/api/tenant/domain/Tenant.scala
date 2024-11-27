package org.example.api.tenant.domain

import org.example.api.types.{Address, Entity}
import zio.schema.DeriveSchema

object Tenant:

  val schema        = DeriveSchema.gen[Tenant]
  val summarySchema = DeriveSchema.gen[TenantSummary]

end Tenant

case class Tenant(override val id: Entity.Id, name: String, address: Address) extends Entity:
  lazy val summary = TenantSummary(id, name)

case class TenantSummary(override val id: Entity.Id, name: String) extends Entity
