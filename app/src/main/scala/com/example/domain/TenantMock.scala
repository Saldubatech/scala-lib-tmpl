package com.example.domain

import com.saldubatech.domain.types.Reference
import com.saldubatech.domain.types.geo.Address
import com.saldubatech.infrastructure.network.Network.{Endpoint, ServiceLocator}
import com.saldubatech.infrastructure.network.oas3.APIError
import com.saldubatech.infrastructure.network.oas3.entity.{Adaptor, EntityResult, PageResult}
import com.saldubatech.infrastructure.network.Network
import com.saldubatech.infrastructure.services.{EntityService, ServiceLocalAddress}
import com.saldubatech.lang.query.Query
import com.saldubatech.lang.Id
import com.saldubatech.lang.types.{AppResult, DIO}
import com.saldubatech.lang.types.datetime.Epoch
import com.saldubatech.lang.types.meta.MetaType
import org.example.tenant.api.oas3.zio as tenantApi
import org.example.tenant.api.oas3.zio.domain.TenantSummary
import org.example.tenant.domain.Tenant
import zio.{IO, ZIO, ZLayer}

object TenantMock:

  import MetaType.given

  def sampleTenant(ep: ServiceLocator, name: String)(using tAPI: EntityResult.API[Tenant]) =
    tAPI.entity(
      EntityResult.Id(ep, Id),
      Tenant(
        Reference(ep.address, Id),
        name,
        Address(firstLine = "1234 Somewhere", secondLine = Some("There"), city = "Oz", region = "Lalaland",
          postalCode = "88-8898", country = "MiddleEarth")
      )
    )

  class Crud(ep: ServiceLocator) extends EntityService[Tenant]:

    override val address: ServiceLocalAddress = ep.address

    val one: IO[AppResult.Error, EntityResult[Tenant]] =
      ZIO.succeed(sampleTenant(ep, s"do One ${java.util.UUID.randomUUID().toString.take(6)}"))

    val page: IO[AppResult.Error, PageResult[Tenant]] =
      ZIO.succeed(
        PageResult(
          "thisOne",
          "previousOne",
          "nextOne",
          List(
            sampleTenant(ep, s"do List #1 ${java.util.UUID.randomUUID().toString.take(6)}"),
            sampleTenant(ep, s"do List #2 ${java.util.UUID.randomUUID().toString.take(6)}")
          )
        )
      )

    val pageSummary: IO[AppResult.Error, PageResult[TenantSummary]] =
      page.map(pr =>
        PageResult(pr.thisPage, pr.previousPage, pr.nextPage, pr.results.map(e => e.map(t => TenantSummary(t.name))))
      )

    override def get(eId: Id): DIO[Tenant] = ???

    override def query(q: Query): DIO[Iterable[Tenant]] = ???

    override def create(e: Tenant): DIO[Tenant] = ???

    override def delete(id: Id): DIO[Tenant] = ???

    override def update(newValue: Tenant): DIO[Tenant] = ???

  end Crud // class

  val crudLayer = ZLayer(ZIO.service[ServiceLocator].map(Crud(_)))

  class MockAdaptor(override val service: Crud, override val forEndpoint: Network.Endpoint)
      extends Adaptor[Tenant, TenantSummary]:

    override def handleQuery(q: Query, requestId: Long): IO[APIError, (PageResult[Tenant], Long)] =
      service.page.mapBoth(errorHandler.map(requestId, Epoch.now, _), success => (success, requestId))

    override def handleGet(id: String, requestId: Long): IO[APIError, (EntityResult[Tenant], Long)] =
      service.one.mapBoth(errorHandler.map(requestId, Epoch.now, _), (_, requestId))

    override def handleCreate(newE: Tenant, requestId: Long): IO[APIError, (EntityResult[Tenant], Long)] = ???

    override def handleDelete(id: String, requestId: Long): IO[APIError, (EntityResult[Tenant], Long)] = ???

    override def handleUpdate(eId: String, newE: Tenant, requestId: Long): IO[APIError, (EntityResult[Tenant], Long)] = ???

    override def handleFind(q: String, requestId: Epoch): IO[APIError, (PageResult[Tenant], Epoch)] =
      service.page.mapBoth(errorHandler.map(requestId, Epoch.now, _), success => (success, requestId))

    override def handleFindSummaries(q: Id, requestId: Epoch)(using s: Adaptor.Summarizer[Tenant, TenantSummary])     = ???
    override def handleQuerySummaries(q: Query, requestId: Epoch)(using s: Adaptor.Summarizer[Tenant, TenantSummary]) = ???

//    override val subscribe: ServiceEndpoint => IO[AppResult.Error, Subject.SubscriptionId] = ep => ???
//    override val unsubscribe: Subject.SubscriptionId => IO[AppResult.Error, Boolean]       = ep => ???

  end MockAdaptor

  val adaptorLayer: ZLayer[Crud & ServiceLocator, AppResult.Error, MockAdaptor] = ZLayer(
    for {
      crd <- ZIO.service[Crud]
      ep  <- ZIO.service[ServiceLocator]
    } yield MockAdaptor(crd, ep.at)
  )

end TenantMock
