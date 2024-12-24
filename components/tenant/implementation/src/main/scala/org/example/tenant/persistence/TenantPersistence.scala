package org.example.tenant.persistence

import com.saldubatech.domain.types.geo.Address
import com.saldubatech.domain.types.Reference
import org.example.tenant.domain.Tenant

object TenantPersistence:
  def apply(t: Tenant): TenantPersistence = TenantPersistence(t.name, t.address)

end TenantPersistence

case class TenantPersistence(name: String, address: Address)
