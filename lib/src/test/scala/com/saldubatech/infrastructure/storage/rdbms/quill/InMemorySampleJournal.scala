package com.saldubatech.infrastructure.storage.rdbms.quill

import com.saldubatech.infrastructure.storage.{
  DIO,
  JournalEntry,
  JournaledDomain,
  NotFoundError,
  Term,
  TimeCoordinates,
  TooManyResultsError
}
import com.saldubatech.lang.Id
import com.saldubatech.lang.types.*
import io.getquill.jdbczio.Quill
import io.getquill.*
import zio.{IO, URLayer, ZIO, ZLayer}

final case class SamplePayload(name: String, price: Double)

object SampleJournal:

  val layer: URLayer[Quill.Postgres[Literal], SampleJournal] = ZLayer(
    for {
      quillCtx <- ZIO.service[Quill.Postgres[Literal]]
    } yield SampleJournal(quillCtx)
  )

end SampleJournal // object

class SampleJournal(quill: Quill.Postgres[Literal])
    extends JournaledDomain[SamplePayload]
    with LinearJournal.Inliner[SamplePayload]("SampleJournal", quill):

  import quill.*

  inline def baseQuery: EntityQuery[JournaledDomain.EntryRecord[SamplePayload]] =
    querySchema[JournaledDomain.EntryRecord[SamplePayload]]("sample_journal")

  override def add(eId: Id, p: SamplePayload, coordinates: TimeCoordinates): DIO[JournalEntry[SamplePayload]] =
    adder(baseQuery)(eId, p, coordinates)

  override def get(eId: Id, at: TimeCoordinates): DIO[JournalEntry[SamplePayload]] = getter(baseQuery, "SampleJournal")(eId, at)

  override def lineage(eId: Id, from: Option[TimeCoordinates], until: Option[TimeCoordinates]) =
    this.lineageGetter(baseQuery, "SampleJournal")(eId, from, until)

  override def get(rId: Id): DIO[JournalEntry[SamplePayload]] = recordGetter(baseQuery)(rId)

  override def findAll(at: TimeCoordinates): DIO[Iterable[JournalEntry[SamplePayload]]] = allFinder(baseQuery)(at)

  override inline def find(inline t: Term[SamplePayload], at: TimeCoordinates): DIO[Iterable[JournalEntry[SamplePayload]]] =
    finder(baseQuery, t)(at)

  override inline def findIncludingRemoved(inline t: Term[SamplePayload], at: TimeCoordinates)
      : DIO[Iterable[JournalEntry[SamplePayload]]] = includingRemovedFinder(baseQuery, t)(at)

  override def countAll(at: TimeCoordinates): DIO[Long] = allCounter(baseQuery)(at)

  override inline def count(inline t: Term[SamplePayload], at: TimeCoordinates): DIO[Long] = counter(baseQuery, t)(at)

  override def update(eId: Id, at: TimeCoordinates, payload: SamplePayload): DIO[JournalEntry[SamplePayload]] =
    updater(baseQuery, "SampleJournal")(eId, at, payload)

  override def update(updated: JournalEntry[SamplePayload]): DIO[JournalEntry[SamplePayload]] =
    journalEntryUpdater(baseQuery, "SampleJournal")(updated)

  override def remove(eId: Id, at: TimeCoordinates): DIO[JournalEntry[SamplePayload]] =
    remover(baseQuery, "SampleJournal")(eId, at)

end SampleJournal // class
