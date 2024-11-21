package com.saldubatech.infrastructure.storage.rdbms.quill

import com.saldubatech.infrastructure.storage.*
import com.saldubatech.infrastructure.storage.JournaledDomain.EntryRecord
import com.saldubatech.lang.Id
import com.saldubatech.lang.types.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.{IO, ZIO}

import java.sql.SQLException

/** Type Safety with Encoders and Decoders: The QuillJournal now uses Encoder and Decoder type parameters for the payload P. This
  * ensures type safety when interacting with the database.
  *
  *   - SchemaMeta: The schemaMeta call ensures the correct table name is used.
  *   - Simplified Adder Logic: Removed the unnecessary double-check of the ID in add. The unique constraint should be enforced at
  *     the database level.
  *   - getTimestamp: Uses provided TimeCoordinates instead.
  *   - get Methods: Provides implementations for both get methods, fetching by rId and by eId with time coordinates. Uses sorting
  *     to ensure latest entry is retrieved.
  *   - Monotonicity Checks in Update: Added checks in update to ensure both recordedAt and effectiveAt are monotonic. Rejects
  *     updates with earlier times using ValidationError.
  *   - Transaction Handling: Uses quillCtx transaction for transactional operations, improving reliability.
  *   - Error Handling: Continues to use the handleExceptions method for consistent error management.
  *   - Placeholder Implementations: Provides placeholders ( ???) for findAll, find, findIncludingRemoved, countAll, count, and
  *     remove since these require more complex logic regarding time travel queries, removal markers and duplicates due to time
  *     branching. These can be implemented in a following step.
  */
object LinearJournal:

  trait Inliner[P <: Payload](journalId: Id, quillCtx: Quill.Postgres[Literal]) extends JournaledDomain[P]:

    import quillCtx.*

    protected inline def adder(inline baseQuery: EntityQuery[EntryRecord[P]]): (Id, P, TimeCoordinates) => DIO[JournalEntry[P]] =
      (eId: Id, p: P, coordinates: TimeCoordinates) =>
        val record = EntryRecord.create(Id, journalId, eId, coordinates, p)
        transaction(
          for {
            existing <- run(quote(baseQuery.filter(_.eId == lift(eId)).map(_.rId).size))
            result <- if existing == 0 then run(quote(baseQuery.insertValue(lift(record)).returning(r => r)))
                      else ZIO.fail(InsertionError(s"Entity with id $eId already exists"))
            rs <- result.toJournalEntry.toZIO
          } yield rs
        ).handleExceptions

    private inline def getQuery(
        inline baseQuery: EntityQuery[EntryRecord[P]],
        inline eId: Id,
        inline at: TimeCoordinates,
        inline jId: Id
      ) =
      baseQuery.filter(r =>
        r.eId == lift(eId) &&
          r.journalId == lift(jId) &&
          r.discriminator != lift(JournalEntry.REMOVAL) &&
          baseQuery
            .filter(ir =>
              ir.recordedAt <= lift(at.recordedAt) &&
                ir.effectiveAt <= lift(at.effectiveAt) &&
                ir.eId == lift(eId)
            )
            .sortBy(r1 => (r1.recordedAt, r1.effectiveAt))(Ord(Ord.descNullsLast, Ord.descNullsLast))
            .map(mr => mr.rId)
            .take(1)
            .contains(r.rId)
      )

    protected inline def getter(inline baseQuery: EntityQuery[EntryRecord[P]], inline jId: Id)
        : (Id, TimeCoordinates) => DIO[JournalEntry[P]] =
      (eId: Id, at: TimeCoordinates) =>
        for {
          qRs <- run(quote(getQuery(baseQuery, eId, at, jId))).handleExceptions
          rs <- qRs match {
                  case Nil      => ZIO.fail(NotFoundError(eId))
                  case r :: Nil => r.toJournalEntry.toZIO
                  case _        => ZIO.fail(TooManyResultsError(eId)) // Should not happen
                }
        } yield rs

    protected inline def lineageGetter(inline baseQuery: EntityQuery[EntryRecord[P]], inline jId: Id)
        : (Id, Option[TimeCoordinates], Option[TimeCoordinates]) => DIO[Iterable[JournalEntry[P]]] =
      (eId: Id, from: Option[TimeCoordinates], until: Option[TimeCoordinates]) =>
        val q: Quoted[Query[EntryRecord[P]]] = (from, until) match
          case (None, None) =>
            quote(
              baseQuery
                .filter(_.eId == lift(eId))
                .sortBy(r1 => (r1.recordedAt, r1.effectiveAt))(Ord(Ord.ascNullsLast, Ord.ascNullsLast))
            )
          case (Some(fTc), None) =>
            quote(
              baseQuery
                .filter(er =>
                  er.eId == lift(eId) && er.recordedAt >= lift(fTc.recordedAt) && er.effectiveAt >= lift(fTc.effectiveAt)
                )
                .sortBy(r1 => (r1.recordedAt, r1.effectiveAt))(Ord(Ord.ascNullsLast, Ord.ascNullsLast))
            )
          case (None, Some(uTc)) =>
            quote(
              baseQuery
                .filter(er =>
                  er.eId == lift(eId) && er.recordedAt <= lift(uTc.recordedAt) && er.effectiveAt <= lift(uTc.effectiveAt)
                )
                .sortBy(r1 => (r1.recordedAt, r1.effectiveAt))(Ord(Ord.ascNullsLast, Ord.ascNullsLast))
            )
          case (Some(fTc), Some(uTc)) =>
            quote(
              baseQuery
                .filter(er =>
                  er.eId == lift(eId) && er.recordedAt >= lift(fTc.recordedAt) && er.effectiveAt >= lift(
                    fTc.effectiveAt
                  ) && er.recordedAt <= lift(uTc.recordedAt) && er.effectiveAt <= lift(uTc.effectiveAt)
                )
                .sortBy(r1 => (r1.recordedAt, r1.effectiveAt))(Ord(Ord.ascNullsLast, Ord.ascNullsLast))
            )
        for {
          qRs <- run(q).handleExceptions
          rs  <- qRs.map(_.toJournalEntry).collectAll.toZIO
        } yield rs

    protected inline def recordGetter(inline baseQuery: EntityQuery[EntryRecord[P]]): Id => DIO[JournalEntry[P]] =
      (rId: Id) =>
        for {
          qRs <- run(quote(baseQuery.filter(_.rId == lift(rId)))).handleExceptions
          rs <- qRs match
                  case r :: Nil => r.toJournalEntry.toZIO
                  case Nil      => ZIO.fail(NotFoundError(rId))
                  case _        => ZIO.fail(TooManyResultsError(rId))
        } yield rs

    protected inline def allFinder(inline baseQuery: EntityQuery[EntryRecord[P]])
        : TimeCoordinates => DIO[Iterable[JournalEntry[P]]] =
      (at: TimeCoordinates) =>
        for {
          latestRecords <-
            run(
              quote(
                baseQuery.filter(r =>
                  r.discriminator != lift(JournalEntry.REMOVAL) &&
                    baseQuery
                      .filter(r =>
                        r.recordedAt <= lift(at.recordedAt) &&
                          r.effectiveAt <= lift(at.effectiveAt)
                      )
                      .groupByMap(_.eId)(entry => (entry.eId, max(entry.recordedAt), max(entry.effectiveAt)))
                      .contains((r.eId, r.recordedAt, r.effectiveAt))
                )
              )
            ).handleExceptions
          journalEntries <- ZIO.foreach(latestRecords)(_.toJournalEntry.toZIO)
        } yield journalEntries

    protected inline def finder(inline baseQuery: EntityQuery[EntryRecord[P]], inline t: Term[P])
        : TimeCoordinates => DIO[Iterable[JournalEntry[P]]] =
      (at: TimeCoordinates) =>
        for {
          latestRecords <-
            run(
              quote(
                baseQuery.filter(r =>
                  r.discriminator != lift(JournalEntry.REMOVAL) &&
                    baseQuery
                      .filter(r =>
                        r.recordedAt <= lift(at.recordedAt) &&
                          r.effectiveAt <= lift(at.effectiveAt) &&
                          t(r.payload)
                      )
                      .groupByMap(_.eId)(entry => (entry.eId, max(entry.recordedAt), max(entry.effectiveAt)))
                      .contains((r.eId, r.recordedAt, r.effectiveAt))
                )
              )
            ).handleExceptions
          journalEntries <- ZIO.foreach(latestRecords)(_.toJournalEntry.toZIO)
        } yield journalEntries

    protected inline def includingRemovedFinder(inline baseQuery: EntityQuery[EntryRecord[P]], inline t: Term[P])
        : TimeCoordinates => DIO[Iterable[JournalEntry[P]]] =
      (at: TimeCoordinates) =>
        for {
          latestRecords <-
            run(
              quote(
                baseQuery.filter(r =>
                  baseQuery
                    .filter(r =>
                      r.recordedAt <= lift(at.recordedAt) &&
                        r.effectiveAt <= lift(at.effectiveAt) &&
                        t(r.payload)
                    )
                    .groupByMap(_.eId)(entry => (entry.eId, max(entry.recordedAt), max(entry.effectiveAt)))
                    .contains((r.eId, r.recordedAt, r.effectiveAt))
                )
              )
            ).handleExceptions
          journalEntries <- ZIO.foreach(latestRecords)(_.toJournalEntry.toZIO)
        } yield journalEntries

    protected inline def allCounter(inline baseQuery: EntityQuery[EntryRecord[P]]): TimeCoordinates => DIO[Long] =
      (at: TimeCoordinates) =>
        for {
          count <-
            run(
              quote(
                baseQuery
                  .filter(r =>
                    r.discriminator != lift(JournalEntry.REMOVAL) &&
                      baseQuery
                        .filter(r =>
                          r.recordedAt <= lift(at.recordedAt) &&
                            r.effectiveAt <= lift(at.effectiveAt)
                        )
                        .groupByMap(_.eId)(entry => (entry.eId, max(entry.recordedAt), max(entry.effectiveAt)))
                        .contains((r.eId, r.recordedAt, r.effectiveAt))
                  )
                  .size
              )
            ).handleExceptions
        } yield count

    protected inline def counter(inline baseQuery: EntityQuery[EntryRecord[P]], inline t: Term[P]): TimeCoordinates => DIO[Long] =
      (at: TimeCoordinates) =>
        for {
          count <-
            run(
              quote(
                baseQuery
                  .filter(r =>
                    r.discriminator != lift(JournalEntry.REMOVAL) &&
                      baseQuery
                        .filter(r =>
                          r.recordedAt <= lift(at.recordedAt) &&
                            r.effectiveAt <= lift(at.effectiveAt) &&
                            t(r.payload)
                        )
                        .groupByMap(_.eId)(entry => (entry.eId, max(entry.recordedAt), max(entry.effectiveAt)))
                        .contains((r.eId, r.recordedAt, r.effectiveAt))
                  )
                  .size
              )
            ).handleExceptions
        } yield count

    protected inline def updater(inline baseQuery: EntityQuery[EntryRecord[P]], inline jId: Id)
        : (Id, TimeCoordinates, P) => DIO[JournalEntry[P]] =
      (eId: Id, at: TimeCoordinates, payload: P) =>
        transaction(
          for {
            tg <- run(quote(getQuery(baseQuery, eId, at, jId).take(1)))
            inserted <-
              tg.headOption.fold(ZIO.fail(NotFoundError(eId)))(r =>
                if at.recordedAt > r.recordedAt && at.effectiveAt > r.effectiveAt then
                  val nr = JournaledDomain
                    .EntryRecord[P](JournalEntry.UPDATE, Id, r.journalId, r.eId, at.recordedAt, at.effectiveAt, payload,
                      Option(r.rId))
                  run(quote(baseQuery.insertValue(lift(nr)).returning(r => r)))
                else ZIO.fail(ValidationError(s"Cannot update unless recordedAt and effectiveAt are monotonic"))
              )
            rs <- inserted.toJournalEntry.toZIO
          } yield rs
        ).handleExceptions

    protected inline def journalEntryUpdater(inline baseQuery: EntityQuery[EntryRecord[P]], inline jId: Id)
        : JournalEntry[P] => DIO[JournalEntry[P]] =
      (baseEntry: JournalEntry[P]) => updater(baseQuery, jId)(baseEntry.eId, baseEntry.coordinates, baseEntry.payload)

    protected inline def remover(inline baseQuery: EntityQuery[EntryRecord[P]], inline jId: Id)
        : (Id, TimeCoordinates) => DIO[JournalEntry[P]] =
      (eId: Id, at: TimeCoordinates) =>
        transaction(
          for {
            tg <- run(quote(getQuery(baseQuery, eId, at, jId).take(1)))
            inserted <-
              tg.headOption.fold(ZIO.fail(NotFoundError(eId)))(r =>
                if at.recordedAt > r.recordedAt && at.effectiveAt > r.effectiveAt then
                  val nr = JournaledDomain
                    .EntryRecord[P](JournalEntry.REMOVAL, Id, r.journalId, r.eId, at.recordedAt, at.effectiveAt, r.payload,
                      Option(r.rId))
                  run(quote(baseQuery.insertValue(lift(nr)).returning(r => r)))
                else ZIO.fail(ValidationError(s"Cannot remove unless recordedAt and effectiveAt are monotonic"))
              )
            rs <- inserted.toJournalEntry.toZIO
          } yield rs
        ).handleExceptions

    extension [RS](o: IO[Throwable, RS])

      def handleExceptions: DIO[RS] =
        o.refineOrDie {
          case e: SQLException      => RepositoryError.fromThrowable(e)
          case pe: PersistenceError => pe
        }

  end Inliner // trait

end LinearJournal
