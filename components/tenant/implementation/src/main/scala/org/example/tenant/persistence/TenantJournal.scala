package org.example.tenant.persistence

import com.saldubatech.infrastructure.storage.{JournalEntry, JournaledDomain, SORT, SORTDIR, Term, TimeCoordinates}
import com.saldubatech.infrastructure.storage.rdbms.quill.{DynamicPredicate, DynamicSort, LinearJournal}
import com.saldubatech.lang.Id
import com.saldubatech.lang.query.Page
import com.saldubatech.lang.types.DIO
import io.getquill.jdbczio.Quill
import io.getquill.*
import zio.{ZIO, ZLayer}

object TenantJournal:

  inline val jId = "TENANT"

  val layer =
    ZLayer(
      for {
        qCtx <- ZIO.service[Quill.Postgres[Literal]]
      } yield TenantJournal(qCtx)
    )

end TenantJournal // object

class TenantJournal(quillCtx: Quill.Postgres[Literal])
    extends JournaledDomain[TenantPersistence, Query, Quoted, Ord, DynamicPredicate[TenantPersistence], DynamicSort[
      TenantPersistence
    ]]
    with LinearJournal.Inliner[TenantPersistence](TenantJournal.jId, quillCtx):

  import quillCtx.*
  import TenantJournal.jId

  def inTx[RS](op: DIO[RS]): DIO[RS] = quillCtx.transaction(op).handleExceptions

  inline def baseQuery: EntityQuery[JournaledDomain.EntryRecord[TenantPersistence]] =
    querySchema[JournaledDomain.EntryRecord[TenantPersistence]]("TENANT")

  override def add(eId: Id, p: TenantPersistence, coordinates: TimeCoordinates): DIO[JournalEntry[TenantPersistence]] =
    adder(baseQuery)(eId, p, coordinates)

  override def get(eId: Id, at: TimeCoordinates): DIO[JournalEntry[TenantPersistence]] = getter(baseQuery, jId)(eId, at)

  override def get(rId: Id): DIO[JournalEntry[TenantPersistence]] = recordGetter(baseQuery)(rId)

  override def lineage(eId: Id, from: Option[TimeCoordinates], until: Option[TimeCoordinates])
      : DIO[Iterable[JournalEntry[TenantPersistence]]] = lineageGetter(baseQuery, jId)(eId, from, until)

  override def findAll(at: TimeCoordinates, page: Page = Page()): DIO[Iterable[JournalEntry[TenantPersistence]]] =
    allFinder(baseQuery, at, page)

  override inline def find(inline t: Term[TenantPersistence], at: TimeCoordinates, page: Page = Page())
      : DIO[Iterable[JournalEntry[TenantPersistence]]] = finder(baseQuery, t)(at, page)

  override inline def findIncludingRemoved(inline t: Term[TenantPersistence], at: TimeCoordinates, page: Page = Page())
      : DIO[Iterable[JournalEntry[TenantPersistence]]] = includingRemovedFinder(baseQuery, t)(at, page)

  override def findDynamicIncludingRemoved(
      dp: DynamicPredicate[TenantPersistence],
      at: TimeCoordinates,
      page: Page = Page()
    ): DIO[Iterable[JournalEntry[TenantPersistence]]] = includingRemovedFinderDynamic(baseQuery)(dp, at, page)

  override def countAll(at: TimeCoordinates): DIO[Long] = allCounter(baseQuery)(at)

  override inline def count(inline t: Term[TenantPersistence], at: TimeCoordinates): DIO[Long] = counter(baseQuery, t)(at)

  override def countDynamic(dp: DynamicPredicate[TenantPersistence], at: TimeCoordinates): DIO[Long] =
    counterDynamic(baseQuery)(dp, at)

  override def update(eId: Id, at: TimeCoordinates, payload: TenantPersistence): DIO[JournalEntry[TenantPersistence]] =
    updater(baseQuery, jId)(eId, at, payload)

  override def update(updated: JournalEntry[TenantPersistence]): DIO[JournalEntry[TenantPersistence]] =
    journalEntryUpdater(baseQuery, jId)(updated)

  override def remove(eId: Id, at: TimeCoordinates): DIO[JournalEntry[TenantPersistence]] = remover(baseQuery, jId)(eId, at)

  override inline def findSorted(
      inline t: Term[TenantPersistence],
      at: TimeCoordinates,
      inline sort: SORT[TenantPersistence],
      inline sortDir: SORTDIR[Ord],
      page: Page = Page()
    ): DIO[Iterable[JournalEntry[TenantPersistence]]] = finderSorted(baseQuery, t)(at, sort, sortDir, page)

  override def findDynamicSorted(
      dp: DynamicPredicate[TenantPersistence],
      at: TimeCoordinates,
      sort: Option[DynamicSort[TenantPersistence]],
      page: Page
    ): DIO[Iterable[JournalEntry[TenantPersistence]]] =
    sort match
      case None       => finderDynamic(baseQuery)(dp, at, page)
      case Some(iSrt) => finderDynamicSorted(baseQuery)(dp, at, iSrt, page)

end TenantJournal // class
