package org.example.api.tenant

import com.saldubatech.lang.types.AppResult
import org.example.api.lib
import org.example.api.lib.bindings.ServiceEndpoint
import org.example.api.lib.requestresponse.{EntityOperations, Subject}
import org.example.api.tenant.domain.{Tenant, TenantSummary}
import zio.{ZIO, ZLayer}
import zio.schema.Schema

object ApiComponent:

  trait Adaptor extends EntityOperations.Adaptor[Tenant, TenantSummary] with Subject.Adaptor

  class Endpoint(override val name: String, override val version: String)
      extends EntityOperations[Tenant, TenantSummary]
      with Subject:

    given eSchema: Schema[Tenant] = Tenant.schema

    given sSchema: Schema[TenantSummary] = Tenant.summarySchema

    override val openAPI = super.openAPI ++ subscriptionOpenAPI

  end Endpoint

  class Implementation(ep: Endpoint)
      extends EntityOperations.Implementation[Tenant, TenantSummary, Adaptor](ep)
      with Subject.Implementation[Adaptor](ep):

    override def routes = super.routes ++ subjectRoutes

  end Implementation //

  object Factory extends lib.ApiComponent.Factory[Tenant, TenantSummary, ApiComponent.Adaptor]:

    val layer: ZLayer[ApiComponent.Adaptor & ServiceEndpoint, AppResult.Error, ApiComponent] =
      ZLayer(
        for {
          ep      <- ZIO.service[ServiceEndpoint]
          adaptor <- ZIO.service[ApiComponent.Adaptor]
        } yield ApiComponent(ep)
      )

  end Factory // object

end ApiComponent

class ApiComponent(ep: ServiceEndpoint) extends lib.ApiComponent[Tenant, TenantSummary, ApiComponent.Adaptor]:

  override type Endpoint       = ApiComponent.Endpoint
  override type Implementation = ApiComponent.Implementation

  override val endpoint: Endpoint = ApiComponent.Endpoint(ep.name, ep.version)

  override val implementation: Implementation = ApiComponent.Implementation(endpoint)

  val routes = implementation.routes ++ implementation.swaggerRoutes

end ApiComponent
