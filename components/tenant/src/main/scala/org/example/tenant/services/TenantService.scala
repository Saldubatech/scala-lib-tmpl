package org.example.tenant.services

import com.saldubatech.domain.types.Reference
import com.saldubatech.infrastructure.network.Network
import com.saldubatech.infrastructure.network.Network.{Endpoint, ServiceLocator}
import com.saldubatech.infrastructure.services.{EntityService, ServiceLocalAddress}
import com.saldubatech.infrastructure.storage.{JournalEntry, TimeCoordinates}
import com.saldubatech.infrastructure.storage.rdbms.quill.{DynamicPredicate, DynamicPredicateFactory, DynamicSortFactory}
import com.saldubatech.lang.Id
import com.saldubatech.lang.types.*
import com.saldubatech.lang.query.{Filter, Query}
import com.saldubatech.lang.types.DIO
import org.example.tenant.domain.Tenant
import org.example.tenant.persistence.{TenantJournal, TenantPersistence}
import zio.{URLayer, ZIO, ZLayer}

object TenantService:

  trait Interface extends EntityService[Tenant]

  val defaultLayer = layer(DynamicPredicateFactory[TenantPersistence](), DynamicSortFactory[TenantPersistence]())

  def layer(dpf: DynamicPredicateFactory[TenantPersistence], dps: DynamicSortFactory[TenantPersistence])
      : URLayer[TenantJournal & ServiceLocator, TenantService.Interface] =
    ZLayer(
      for {
        locator     <- ZIO.service[ServiceLocator]
        persistence <- ZIO.service[TenantJournal]
      } yield TenantService(dpf, dps)(locator.address, persistence)
    )

end TenantService // object

class TenantService(
    dpf: DynamicPredicateFactory[TenantPersistence],
    dps: DynamicSortFactory[TenantPersistence]
  )(override val address: ServiceLocalAddress,
    persistence: TenantJournal)
    extends TenantService.Interface:

  private inline def fromPersistence(localId: Id, r: TenantPersistence): Tenant =
    Tenant(Reference(address, localId), r.name, r.address)

  private inline def toPersistence(t: Tenant): TenantPersistence = TenantPersistence(t.name, t.address)

  def get(eId: Id): DIO[Tenant] =
    persistence.inTx(
      persistence.get(eId, TimeCoordinates.now).map(je => fromPersistence(eId, je.payload))
    )

  def query(q: Query): DIO[Iterable[Tenant]] =
    for {
      iDp  <- dpf.journaled(q.filter.getOrElse(Filter.Literal.TRUE)).toZIO
      iSrt <- q.order.fold(AppResult.Success(None))(dps.apply(_).map(Some(_))).toZIO
      rs <-
        persistence
          .findDynamicSorted(iDp, TimeCoordinates.now, iSrt, q.page)
          .map(rs => rs.map(je => fromPersistence(je.eId, je.payload)))
    } yield rs

  // Just for kicks to show something different from the Journal and show the use of transactions scopes.
  def duplicate(tId: Id): DIO[Tenant] =
    persistence.inTx(
      for {
        t  <- get(tId)
        rs <- create(t.copy(locator = Reference(address, Id)))
      } yield rs
    )

  def create(e: Tenant): DIO[Tenant] =
    persistence.add(e.locator.localId, toPersistence(e), TimeCoordinates.now).map(je => fromPersistence(je.eId, je.payload))

  def delete(id: Id): DIO[Tenant] = persistence.remove(id, TimeCoordinates.now).map(je => fromPersistence(je.eId, je.payload))

  def update(newValue: Tenant): DIO[Tenant] =
    persistence
      .update(newValue.locator.localId, TimeCoordinates.now, toPersistence(newValue))
      .map(je => fromPersistence(je.eId, je.payload))

end TenantService // class
