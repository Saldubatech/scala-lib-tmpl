package org.example.tenant

import org.example.tenant.api.oas3.zio.TenantOas3
import org.example.tenant.services.TenantService

object TenantComponent extends TenantOas3[TenantService.Interface]:
end TenantComponent
