package org.example.tenant.api.oas3.zio

import com.saldubatech.infrastructure.network.oas3.entity.{EntityEndpoint, Oas3Component, Adaptor as Oas3Adaptor, Routes as Oas3Routes}
import com.saldubatech.infrastructure.network.Network
import org.example.tenant.api.oas3.zio.domain.TenantSummary
import org.example.tenant.component.services.TenantService
import org.example.tenant.domain.Tenant

object TenantOas3Component:

  class Endpoint(override val name: String, override val version: String) extends EntityEndpoint[Tenant, TenantSummary]

  class Adaptor(override val service: TenantService.Interface, override val forEndpoint: Network.Endpoint)
      extends Oas3Adaptor[Tenant, TenantSummary]

  class Routes(ep: Endpoint) extends Oas3Routes[Tenant, TenantSummary, Adaptor](ep)

  object component
      extends Oas3Component[Tenant, TenantSummary, TenantService.Interface, Endpoint, Adaptor, Routes](
        (name, version) => Endpoint.apply(name, version),
        (service, nwEp) => Adaptor.apply(service, nwEp),
        ep => Routes.apply(ep)
      )

  export component.*

end TenantOas3Component
