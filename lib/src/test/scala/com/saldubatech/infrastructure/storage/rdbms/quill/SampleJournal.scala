package com.saldubatech.infrastructure.storage.rdbms.quill

import com.saldubatech.infrastructure.storage.{
  JournalEntry,
  JournaledDomain,
  NotFoundError,
  PRJ,
  Predicate,
  SORT,
  SORTDIR,
  Sort,
  Term,
  TimeCoordinates,
  TooManyResultsError
}
import com.saldubatech.infrastructure.storage.JournaledDomain.EntryRecord
import com.saldubatech.lang.Id
import com.saldubatech.lang.query.{Filter, Interval, Page, Projectable, Projection, ValueType}
import com.saldubatech.lang.types.*
import com.saldubatech.lang.types.datetime.Epoch
import io.getquill.jdbczio.Quill
import io.getquill.*
import zio.{IO, URLayer, ZIO, ZLayer}

import scala.reflect.Typeable

final case class SamplePayload(name: String, price: Double)

object SampleJournal:

  val layer: URLayer[Quill.Postgres[Literal], SampleJournal] = ZLayer(
    for {
      quillCtx <- ZIO.service[Quill.Postgres[Literal]]
    } yield SampleJournal(quillCtx)
  )

end SampleJournal // object

class SampleJournal(quill: Quill.Postgres[Literal])
    extends JournaledDomain[SamplePayload, Query, Quoted, Ord, DynamicPredicate[SamplePayload], DynamicSort[SamplePayload]]
    with LinearJournal.Inliner[SamplePayload]("SampleJournal", quill):

  import quill.*
  import com.saldubatech.infrastructure.storage.Sort.*

  inline def bq[P <: Product & Serializable](
      inline tbl: String
    ): EntityQuery[JournaledDomain.EntryRecord[P]] = querySchema[JournaledDomain.EntryRecord[P]](tbl)

  inline def baseQuery: EntityQuery[JournaledDomain.EntryRecord[SamplePayload]] =
    querySchema[JournaledDomain.EntryRecord[SamplePayload]]("sample_journal")

  override def add(eId: Id, p: SamplePayload, coordinates: TimeCoordinates): DIO[JournalEntry[SamplePayload]] =
    adder(baseQuery)(eId, p, coordinates)

  override def get(eId: Id, at: TimeCoordinates): DIO[JournalEntry[SamplePayload]] = getter(baseQuery, "SampleJournal")(eId, at)

  override def lineage(eId: Id, from: Option[TimeCoordinates], until: Option[TimeCoordinates]) =
    this.lineageGetter(baseQuery, "SampleJournal")(eId, from, until)

  override def get(rId: Id): DIO[JournalEntry[SamplePayload]] = recordGetter(baseQuery)(rId)

  override def findAll(at: TimeCoordinates, page: Page = Page()): DIO[Iterable[JournalEntry[SamplePayload]]] =
    allFinder(baseQuery, at, page)

  inline def findAllPaginated(
      inline at: TimeCoordinates,
      inline page: Page = Page()
    ): DIO[Iterable[JournalEntry[SamplePayload]]] = allFinder(baseQuery, at, page)

  inline def findAllSortedPaginated(
      inline at: TimeCoordinates,
      inline sort: SORT[SamplePayload],
      inline sortDir: SORTDIR[Ord],
      inline page: Page = Page()
    ): DIO[Iterable[JournalEntry[SamplePayload]]] = allSortedFinder(baseQuery, at, sort, sortDir, page)

  override inline def find(inline t: Term[SamplePayload], at: TimeCoordinates, page: Page = Page())
      : DIO[Iterable[JournalEntry[SamplePayload]]] = finder(baseQuery, t)(at, page)

  override inline def findSorted(
      inline t: Term[SamplePayload],
      at: TimeCoordinates,
      inline sort: SORT[SamplePayload],
      inline sortDir: SORTDIR[Ord],
      page: Page = Page()
    ): DIO[Iterable[JournalEntry[SamplePayload]]] = finderSorted(baseQuery, t)(at, sort, sortDir, page)

  override def findDynamicSorted(
      dp: DynamicPredicate[SamplePayload],
      at: TimeCoordinates,
      sort: Option[DynamicSort[SamplePayload]],
      page: Page = Page()
    ): DIO[Iterable[JournalEntry[SamplePayload]]] =
    sort match
      case None       => finderDynamic(baseQuery)(dp, at, page)
      case Some(iSrt) => finderDynamicSorted(baseQuery)(dp, at, iSrt, page)

  override inline def findIncludingRemoved(inline t: Term[SamplePayload], at: TimeCoordinates, page: Page = Page())
      : DIO[Iterable[JournalEntry[SamplePayload]]] = includingRemovedFinder(baseQuery, t)(at, page)

  override def findDynamicIncludingRemoved(dp: DynamicPredicate[SamplePayload], at: TimeCoordinates, page: Page = Page())
      : DIO[Iterable[JournalEntry[SamplePayload]]] = includingRemovedFinderDynamic(baseQuery)(dp, at, page)

  override def countAll(at: TimeCoordinates): DIO[Long] = allCounter(baseQuery)(at)

  override inline def count(inline t: Term[SamplePayload], at: TimeCoordinates): DIO[Long] = counter(baseQuery, t)(at)

  override def countDynamic(dp: DynamicPredicate[SamplePayload], at: TimeCoordinates): DIO[Long] =
    counterDynamic(baseQuery)(dp, at)

  override def update(eId: Id, at: TimeCoordinates, payload: SamplePayload): DIO[JournalEntry[SamplePayload]] =
    updater(baseQuery, "SampleJournal")(eId, at, payload)

  override def update(updated: JournalEntry[SamplePayload]): DIO[JournalEntry[SamplePayload]] =
    journalEntryUpdater(baseQuery, "SampleJournal")(updated)

  override def remove(eId: Id, at: TimeCoordinates): DIO[JournalEntry[SamplePayload]] =
    remover(baseQuery, "SampleJournal")(eId, at)

end SampleJournal // class
