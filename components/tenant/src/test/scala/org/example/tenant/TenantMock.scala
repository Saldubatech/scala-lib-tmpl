package org.example.tenant

import com.saldubatech.domain.types.geo.Address
import com.saldubatech.domain.types.Reference
import com.saldubatech.infrastructure.network.oas3.entity.{EntityResult, PageResult}
import com.saldubatech.infrastructure.network.Network
import com.saldubatech.infrastructure.network.Network.ServiceLocator
import com.saldubatech.infrastructure.services.ServiceLocalAddress
import com.saldubatech.lang.query.Query
import com.saldubatech.lang.types.{AppResult, DIO}
import com.saldubatech.lang.types.meta.MetaType
import com.saldubatech.lang.Id
import org.example.tenant.api.oas3.zio.domain.TenantSummary
import org.example.tenant.domain.Tenant
import org.example.tenant.services.TenantService
import zio.{IO, ZIO, ZLayer}

object TenantMock:

  import MetaType.given

  def sampleTenant(ep: ServiceLocator, name: String) =
    Tenant(
      Reference(ep.address, name),
      name,
      Address(firstLine = "1234 Somewhere", secondLine = Some("There"), city = "Oz", region = "Lalaland", postalCode = "88-8898",
        country = "MiddleEarth")
    )

  def sampleTenantEntity(ep: ServiceLocator, name: String)(using tAPI: EntityResult.API[Tenant]) =
    val rs = tAPI.entity(
      EntityResult.Id(ep, Id),
      sampleTenant(ep, name)
    )
    rs

  class Crud(ep: ServiceLocator) extends TenantService.Interface:

    val one: IO[AppResult.Error, EntityResult[Tenant]] =
      ZIO.succeed(sampleTenantEntity(ep, s"do One"))

    val page: IO[AppResult.Error, PageResult[Tenant]] =
      ZIO.succeed(
        PageResult(
          "thisOne",
          "previousOne",
          "nextOne",
          List(
            sampleTenantEntity(ep, s"do List #1 ${java.util.UUID.randomUUID().toString.take(6)}"),
            sampleTenantEntity(ep, s"do List #2 ${java.util.UUID.randomUUID().toString.take(6)}")
          )
        )
      )

    val pageSummary: IO[AppResult.Error, PageResult[TenantSummary]] =
      page.map(pr =>
        PageResult(pr.thisPage, pr.previousPage, pr.nextPage, pr.results.map(e => e.map((t: Tenant) => TenantSummary(t.name))))
      )

    override val address: ServiceLocalAddress = ep.address

    override def get(eId: Id): DIO[Tenant] = ZIO.succeed(sampleTenant(ep, s"do One"))

    override def query(q: Query): DIO[Iterable[Tenant]] = ???

    override def create(e: Tenant): DIO[Tenant] = ???

    override def delete(id: Id): DIO[Tenant] = ???

    override def update(newValue: Tenant): DIO[Tenant] = ???

  end Crud // class

  val crudLayer = ZLayer(ZIO.service[ServiceLocator].map(Crud(_)))

//  class MockAdaptor(override val service: Crud, override val forEndpoint: Network.Endpoint)
//      extends Adaptor[Tenant, TenantSummary]:
//
//    override def handleQuery(q: Query, requestId: Long): IO[APIError, (PageResult[Tenant], Long)] =
//      service.page.mapBoth(errorHandler.map(requestId, Epoch.now, _), success => (success, requestId))
//
//    override def handleGet(id: String, requestId: Long): IO[APIError, (Entity[Tenant], Long)] =
//      service.one.mapBoth(
//        errorHandler.map(requestId, Epoch.now, _),
//        rs => (rs, requestId)
//      )
//
//    override def handleCreate(newE: Tenant, requestId: Long): IO[APIError, (Entity[Tenant], Long)] = ???
//
//    override def handleDelete(id: String, requestId: Long): IO[APIError, (Entity[Tenant], Long)] = ???
//
//    override def handleUpdate(eId: String, newE: Tenant, requestId: Long): IO[APIError, (Entity[Tenant], Long)] = ???
//
//    override def handleFindSummaries(q: String, requestId: Long): IO[APIError, (PageResult[TenantSummary], Long)] = ???
//
//    override def handleFind(q: String, requestId: Epoch): IO[APIError, (PageResult[Tenant], Epoch)] =
//      service.page.mapBoth(errorHandler.map(requestId, Epoch.now, _), success => (success, requestId))
//
//    override def handleQuerySummaries(q: Query, requestId: Epoch): IO[APIError, (PageResult[TenantSummary], Epoch)] = ???
//
////    override val subscribe: ServiceEndpoint => IO[AppResult.Error, Subject.SubscriptionId] = ep => ???
////    override val unsubscribe: Subject.SubscriptionId => IO[AppResult.Error, Boolean]       = ep => ???
//
//  end MockAdaptor

//  val adaptorLayer: ZLayer[Crud & ServiceLocator, AppResult.Error, MockAdaptor] = ZLayer(
//    for {
//      crd <- ZIO.service[Crud]
//      ep  <- ZIO.service[ServiceLocator]
//    } yield MockAdaptor(crd, ep.at)
//  )

end TenantMock
