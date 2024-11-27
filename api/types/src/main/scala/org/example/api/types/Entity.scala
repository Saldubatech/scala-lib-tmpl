package org.example.api.types

import org.example.api.lib.bindings.ServiceEndpoint
import org.example.api.lib.types.UUID

object Entity:
  case class Id(home: ServiceEndpoint, localId: UUID)
end Entity

trait Entity:
  val id: Entity.Id
end Entity // trait
