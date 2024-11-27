package com.example.domain

import com.saldubatech.lang.types.AppResult
import com.saldubatech.lang.types.datetime.Epoch
import org.example.api.lib.bindings.ServiceEndpoint
import org.example.api.lib.requestresponse.{APIError, EntityOperations, Subject}
import org.example.api.lib.requestresponse.EntityOperations.PageResult
import org.example.api.lib.types.UUID
import org.example.api.lib.types.query.Query
import org.example.api.tenant.ApiComponent
import org.example.api.tenant.domain.{Tenant, TenantSummary}
import org.example.api.types.{Address, Entity}
import zio.{IO, ZIO, ZLayer}

object TenantMock:

  def sampleTenant(ep: ServiceEndpoint, name: String) =
    val id = Entity.Id(ep, UUID())
    Tenant(
      id,
      name,
      Address(firstLine = "1234 Somewhere", secondLine = Some("There"), city = "Oz", region = "Lalaland", postalCode = "88-8898",
        country = "MiddleEarth")
    )

  class Crud(ep: ServiceEndpoint):

    val one: IO[AppResult.Error, Tenant] =
      ZIO.succeed(sampleTenant(ep, s"do One ${java.util.UUID.randomUUID().toString.take(6)}"))

    val page: IO[AppResult.Error, EntityOperations.PageResult[Tenant]] =
      ZIO.succeed(
        EntityOperations.PageResult(
          "thisOne",
          "previousOne",
          "nextOne",
          List(
            sampleTenant(ep, s"do List #1 ${java.util.UUID.randomUUID().toString.take(6)}"),
            sampleTenant(ep, s"do List #2 ${java.util.UUID.randomUUID().toString.take(6)}")
          )
        )
      )

    val pageSummary: IO[AppResult.Error, EntityOperations.PageResult[TenantSummary]] =
      page.map(pr => EntityOperations.PageResult(pr.thisPage, pr.previousPage, pr.nextPage, pr.results.map(_.summary)))

  end Crud // class

  val crudLayer: ZLayer[ServiceEndpoint, AppResult.Error, Crud] = ZLayer(ZIO.service[ServiceEndpoint].map(Crud(_)))

  class Adaptor(service: Crud, override val forEndpoint: ServiceEndpoint) extends ApiComponent.Adaptor:

    private val errorHandler = APIError.Mapper(forEndpoint)

    override def handleQuery(q: Query, requestId: Long): IO[APIError, (EntityOperations.PageResult[Tenant], Long)] =
      service.page.mapBoth(errorHandler.map(requestId, Epoch.now, _), success => (success, requestId))

    override def getHandler(id: String, requestId: Long): IO[APIError, (Tenant, Long)] =
      service.one.mapBoth(errorHandler.map(requestId, Epoch.now, _), (_, requestId))

    override def createHandler(newE: Tenant, requestId: Long): IO[APIError, (Tenant, Long)] = ???

    override def deleteHandler(id: String, requestId: Long): IO[APIError, (Tenant, Long)] = ???

    override def updateHandler(eId: String, newE: Tenant, requestId: Long): IO[APIError, (Tenant, Long)] = ???

    override def handleFindSummaries(q: String, requestId: Long)
        : IO[APIError, (EntityOperations.PageResult[TenantSummary], Long)] = ???

    override def handleFind(q: String, requestId: Epoch): IO[APIError, (PageResult[Tenant], Epoch)] =
      service.page.mapBoth(errorHandler.map(requestId, Epoch.now, _), success => (success, requestId))

    override def handleQuerySummaries(q: Query, requestId: Epoch): IO[APIError, (PageResult[TenantSummary], Epoch)] = ???

    override val subscribe: ServiceEndpoint => IO[AppResult.Error, Subject.SubscriptionId] = ep => ???
    override val unsubscribe: Subject.SubscriptionId => IO[AppResult.Error, Boolean]       = ep => ???

  end Adaptor

  val adaptorLayer: ZLayer[Crud & ServiceEndpoint, AppResult.Error, Adaptor] = ZLayer(
    for {
      crd <- ZIO.service[Crud]
      ep  <- ZIO.service[ServiceEndpoint]
    } yield Adaptor(crd, ep)
  )

end TenantMock
