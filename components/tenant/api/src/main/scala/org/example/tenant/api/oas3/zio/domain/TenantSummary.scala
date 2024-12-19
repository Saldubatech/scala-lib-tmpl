package org.example.tenant.api.oas3.zio.domain

import com.saldubatech.lang.types.meta.MetaType

object TenantSummary:

end TenantSummary

case class TenantSummary(name: String) derives MetaType
