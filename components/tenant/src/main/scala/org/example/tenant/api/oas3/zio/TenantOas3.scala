package org.example.tenant.api.oas3.zio

import com.saldubatech.infrastructure.network.oas3.entity.Oas3Component
import com.saldubatech.infrastructure.services.EntityService
import org.example.tenant.api.oas3.zio.domain.TenantSummary
import org.example.tenant.domain.Tenant

object TenantOas3:

end TenantOas3

abstract class TenantOas3[SVC <: EntityService[Tenant]] extends Oas3Component[Tenant, TenantSummary]:

  final override type ES = SVC

end TenantOas3
