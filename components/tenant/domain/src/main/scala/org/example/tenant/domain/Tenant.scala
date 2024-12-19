package org.example.tenant.domain

import com.saldubatech.domain.types.Reference
import com.saldubatech.domain.types.geo.Address
import com.saldubatech.infrastructure.network.Network.{Endpoint, Host}
import com.saldubatech.infrastructure.services.Entity
import com.saldubatech.lang.types.meta.MetaType

object Tenant:

end Tenant

case class Tenant(locator: Reference, name: String, address: Address) extends Entity derives MetaType
