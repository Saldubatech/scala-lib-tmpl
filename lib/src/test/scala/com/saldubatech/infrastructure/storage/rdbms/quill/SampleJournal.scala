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

  /*
    SELECT r. discriminator, r. rId, r. journalId, r. eId, r. recordedAt, r. effectiveAt, r. name, r. price, r. previous FROM sample_journal r
    WHERE r. eId = ? AND r. discriminator <> ? AND r. rId IN (
      SELECT ir. rId FROM sample_journal ir
      WHERE ir. recordedAt <= ? AND ir. effectiveAt <= ? AND ir. eId = ?
      ORDER BY ir. recordedAt DESC NULLS LAST, ir. effectiveAt DESC NULLS LAST LIMIT 1)
   */
//  def get2(eId: Id, at: TimeCoordinates): DIO[JournalEntry[SamplePayload]] =
//    for {
//      qRs <- run(
//               baseQuery.filter(r =>
//                 r.eId == lift(eId) &&
//                   r.discriminator != lift(JournalEntry.REMOVAL) &&
//                   baseQuery
//                     .filter(ir =>
//                       ir.recordedAt <= lift(at.recordedAt) &&
//                         ir.effectiveAt <= lift(at.effectiveAt) &&
//                         ir.eId == lift(eId)
//                     )
//                     .sortBy(r1 => (r1.recordedAt, r1.effectiveAt))(Ord(Ord.descNullsLast, Ord.descNullsLast))
//                     .map(mr => mr.rId)
//                     .take(1)
//                     .contains(r.rId)
//               )
//             ).handleExceptions
//      rs <- qRs match {
//              case Nil      => ZIO.fail(NotFoundError(eId))
//              case r :: Nil => r.toJournalEntry.toZIO
//              case _        => ZIO.fail(TooManyResultsError(eId)) // Should not happen
//            }
//    } yield rs

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
