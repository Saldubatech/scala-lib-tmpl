package com.saldubatech.infrastructure.storage.memory

import com.saldubatech.infrastructure.storage.{
  InsertionError,
  JournalEntry,
  JournaledDomain,
  NotFoundError,
  Payload,
  SORT,
  SORTDIR,
  Term,
  TimeCoordinates,
  ValidationError
}
import com.saldubatech.infrastructure.storage.JournaledDomain.EntryRecord
import com.saldubatech.infrastructure.storage.memory.{DynamicPredicate, DynamicSort}
import com.saldubatech.lang.Id
import com.saldubatech.lang.query.Page
import com.saldubatech.lang.types.*
import zio.ZIO

object LinearJournal:

  /** @param journalId
    * @param storage:
    *   Map[Id, Map[Id, JournalEntry[P]] indexed by eId and rId in order
    * @tparam P
    */
  abstract class Service[P <: Payload](val journalId: Id, storage: collection.mutable.Map[Id, Map[Id, EntryRecord[P]]])
      extends JournaledDomain[P, DynamicPredicate.NOOP, DynamicPredicate.NOOP, DynamicPredicate.NOOP, DynamicPredicate[
        P
      ], DynamicSort[P]]:

    override def add(eId: Id, p: P, coordinates: TimeCoordinates): DIO[JournalEntry[P]] =
      storage.get(eId) match
        case None =>
          val je = JournalEntry.Creation(Id, journalId, eId, coordinates, p)
          storage += eId -> Map(je.rId -> EntryRecord(je))
          ZIO.succeed(je)
        case Some(_) => ZIO.fail(InsertionError(s"Entity with id $eId already exists"))

    override def get(eId: Id, at: TimeCoordinates): DIO[JournalEntry[P]] =
      storage
        .get(eId)
        .flatMap {
          _.values
            .filter(er => er.recordedAt <= at.recordedAt && er.effectiveAt <= at.effectiveAt)
            .toList
            .sortBy(er => TimeCoordinates(er.recordedAt, er.effectiveAt))(TimeCoordinates.Ordering().reverse)
            .headOption
        }
        .fold(ZIO.fail(NotFoundError(eId)))(er =>
          if er.discriminator == JournalEntry.REMOVAL then ZIO.fail(NotFoundError(er.eId))
          else er.toJournalEntry.toZIO
        )

    override def lineage(eId: Id, from: Option[TimeCoordinates], until: Option[TimeCoordinates]): DIO[Iterable[JournalEntry[P]]] =
      storage
        .get(eId)
        .map { lMap =>
          lMap.values.filter { er =>
            val ref = TimeCoordinates(er.recordedAt, er.effectiveAt)
            from.fold(true)(tc => tc.isVisibleFrom(ref)) && until.fold(true)(tc => ref.isVisibleFrom(tc))
          }.toList.sortBy(er => TimeCoordinates(er.recordedAt, er.effectiveAt))(TimeCoordinates.Ordering())
        }
        .fold(
          ZIO.fail(NotFoundError(eId))
        )(l => l.map(_.toJournalEntry).collectAll.toZIO)

    override def get(rId: Id): DIO[JournalEntry[P]] =
      storage.values
        .find(rMap => rMap.keySet(rId))
        .map(rMap => rMap(rId))
        .fold(ZIO.fail(NotFoundError(rId)))(_.toJournalEntry.toZIO)

    override def findAll(at: TimeCoordinates, page: Page = Page()): DIO[Iterable[JournalEntry[P]]] =
      (for {
        entityMap <- storage.values
        candidate <- entityMap.values.toList
                       .sortBy(er => TimeCoordinates(er.recordedAt, er.effectiveAt))(TimeCoordinates.Ordering().reverse)
                       .headOption
        rs <- if candidate.discriminator != JournalEntry.REMOVAL then Some(candidate) else None
      } yield rs.toJournalEntry).collectAll.toZIO

    override inline def find(inline t: Term[P], at: TimeCoordinates, page: Page = Page()): DIO[Iterable[JournalEntry[P]]] =
      val selected = for {
        entityMap <- storage.values
        candidate <- entityMap.values.toList
                       .sortBy(er => TimeCoordinates(er.recordedAt, er.effectiveAt))(TimeCoordinates.Ordering().reverse)
                       .headOption
        rs <- if candidate.discriminator != JournalEntry.REMOVAL && t(candidate.payload) then Some(candidate) else None
      } yield rs.toJournalEntry
      selected.collectAll.toZIO

    override inline def findIncludingRemoved(inline t: Term[P], at: TimeCoordinates, page: Page = Page())
        : DIO[Iterable[JournalEntry[P]]] =
      val selected = for {
        entityMap <- storage.values
        candidate <- entityMap.values.toList
                       .sortBy(er => TimeCoordinates(er.recordedAt, er.effectiveAt))(TimeCoordinates.Ordering().reverse)
                       .headOption
        rs <- if t(candidate.payload) then Some(candidate) else None
      } yield rs.toJournalEntry
      selected.collectAll.toZIO

    override def findDynamicIncludingRemoved(dp: DynamicPredicate[P], at: TimeCoordinates, page: Page = Page())
        : DIO[Iterable[JournalEntry[P]]] = ???

    override def countAll(at: TimeCoordinates): DIO[Long] =
      val selected = for {
        entityMap <- storage.values
        candidate <- entityMap.values.toList
                       .sortBy(er => TimeCoordinates(er.recordedAt, er.effectiveAt))(TimeCoordinates.Ordering().reverse)
                       .headOption
        rs <- if candidate.discriminator != JournalEntry.REMOVAL then Some(candidate) else None
      } yield rs.toJournalEntry
      selected.collectAll.toZIO.map(_.size)

    override inline def count(inline t: Term[P], at: TimeCoordinates): DIO[Long] =
      val selected = for {
        entityMap <- storage.values
        candidate <- entityMap.values.toList
                       .sortBy(er => TimeCoordinates(er.recordedAt, er.effectiveAt))(TimeCoordinates.Ordering().reverse)
                       .headOption
        rs <- if candidate.discriminator != JournalEntry.REMOVAL && t(candidate.payload) then Some(candidate) else None
      } yield rs.toJournalEntry
      selected.collectAll.toZIO.map(_.size)

    override def countDynamic(dp: DynamicPredicate[P], at: TimeCoordinates): DIO[Long] = ???

    override def update(eId: Id, at: TimeCoordinates, payload: P): DIO[JournalEntry[P]] =
      for {
        candidate <- get(eId, at)
        update <- if candidate.coordinates.effectiveAt >= at.effectiveAt || candidate.coordinates.recordedAt >= at.effectiveAt
                  then ZIO.fail(ValidationError("Cannot update unless recordedAt and effectiveAt are monotonic"))
                  else candidate.update(payload, at).toZIO
      } yield
        val er = EntryRecord(update)
        storage(er.eId) += er.rId -> er
        update

    override def update(updated: JournalEntry[P]): DIO[JournalEntry[P]] =
      update(updated.eId, updated.coordinates, updated.payload)

    override def remove(eId: Id, at: TimeCoordinates): DIO[JournalEntry[P]] =
      for {
        candidate <- get(eId, at)
        removal <- if candidate.coordinates.effectiveAt >= at.effectiveAt || candidate.coordinates.recordedAt >= at.effectiveAt
                   then ZIO.fail(ValidationError("Cannot remove unless recordedAt and effectiveAt are monotonic"))
                   else candidate.removal(at).toZIO
      } yield
        val er = EntryRecord(removal)
        storage(er.eId) += er.rId -> er
        removal

    override inline def findSorted(
        inline t: Term[P],
        at: TimeCoordinates,
        inline sort: SORT[P],
        inline sortDir: SORTDIR[DynamicPredicate.NOOP],
        page: Page
      ) = ???

    override def findDynamicSorted(
        dp: DynamicPredicate[P],
        at: TimeCoordinates,
        sort: Option[DynamicSort[P]],
        page: Page
      ): DIO[Iterable[JournalEntry[P]]] = ???

  end Service // class

end LinearJournal // object
