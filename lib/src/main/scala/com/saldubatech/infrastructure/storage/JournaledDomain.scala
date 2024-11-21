package com.saldubatech.infrastructure.storage

import com.saldubatech.lang.Id
import com.saldubatech.lang.types.datetime.Epoch
import com.saldubatech.lang.types.AppResult

object JournaledDomain:

  import JournalEntry.*

  case class EntryRecord[P <: Payload](
      discriminator: String,
      rId: Id,
      journalId: Id,
      eId: Id,
      recordedAt: Epoch,
      effectiveAt: Epoch,
      payload: P,
      previous: Option[Id])
      extends DataRecord:

    lazy val toJournalEntry: AppResult[JournalEntry[P]] =
      discriminator match
        case CREATION => AppResult.Success(Creation(rId, journalId, eId, TimeCoordinates(recordedAt, effectiveAt), payload))
        case UPDATE =>
          AppResult.Success(Update(rId, journalId, eId, TimeCoordinates(recordedAt, effectiveAt), payload, previous.get))
        case REMOVAL =>
          AppResult.Success(Removal(rId, journalId, eId, TimeCoordinates(recordedAt, effectiveAt), payload, previous.get))
        case other => AppResult.fail(s"$other is not a valid discriminator for EntryRecord")

  end EntryRecord // case class

  object EntryRecord:

    def apply[P <: Payload](je: JournalEntry[P]): EntryRecord[P] =
      je match
        case Creation(rId, journalId, eId, tc, payload)          => create(rId, journalId, eId, tc, payload)
        case Update(rId, journalId, eId, tc, payload, previous)  => update(rId, journalId, eId, tc, payload, previous)
        case Removal(rId, journalId, eId, tc, payload, previous) => remove(rId, journalId, eId, tc, payload, previous)

    def create[P <: Payload](rId: Id, journalId: Id, eId: Id, tc: TimeCoordinates, payload: P): EntryRecord[P] =
      EntryRecord(CREATION, rId, journalId, eId, tc.recordedAt, tc.effectiveAt, payload, None)

    def update[P <: Payload](
        rId: Id,
        journalId: Id,
        eId: Id,
        tc: TimeCoordinates,
        payload: P,
        previous: Id
      ): EntryRecord[P] = EntryRecord(UPDATE, rId, journalId, eId, tc.recordedAt, tc.effectiveAt, payload, Some(previous))

    def remove[P <: Payload](
        rId: Id,
        journalId: Id,
        eId: Id,
        tc: TimeCoordinates,
        payload: P,
        previous: Id
      ): EntryRecord[P] = EntryRecord(REMOVAL, rId, journalId, eId, tc.recordedAt, tc.effectiveAt, payload, Some(previous))

  end EntryRecord // object

end JournaledDomain // object

trait JournaledDomain[P <: Payload]:

  /** Add a new entity to the domain, creating a JournalEntry for it with a new recordId and the given time coordinates.
    *
    * The method checks the entity Id for uniqueness and returns a failed DIO with a Validation error if not unique.
    *
    * @param eid
    * @param p
    * @param coordinates
    * @return
    */
  def add(eId: Id, p: P, coordinates: TimeCoordinates): DIO[JournalEntry[P]]

  /** Get the JournalEntry for the entity identified by the Id that is the most recent before the cut-off times specified by the
    * time coordinates. If no such record exists, it returns a failure with a NotFoundError.
    * @param eId
    * @param at
    * @return
    */
  def get(eId: Id, at: TimeCoordinates): DIO[JournalEntry[P]]

  /** Get the JournalEntry identified with rId, if it exists or return a failure with a NotFoundError.
    * @param rId
    * @return
    */
  def get(rId: Id): DIO[JournalEntry[P]]

  def lineage(eId: Id, from: Option[TimeCoordinates], until: Option[TimeCoordinates]): DIO[Iterable[JournalEntry[P]]]

  /** Find one JournalEntry for each entity in the domain. For each entity it will return the most recent record before the given
    * time coordinates. If no records for entities exist, it returns an empty Iterable. Entities that have been deleted before the
    * given time coordinates will not be included in the result.
    *
    * @param at
    * @return
    */
  def findAll(at: TimeCoordinates): DIO[Iterable[JournalEntry[P]]]

  /** Find one JournalEntry for each entity in the domain that makes the Term true. The record for each entity is the most recent
    * before the given time coordinates.
    *
    * This method is inline to support Quill macros to translate Scala terms into SQL.
    * @param t
    * @param at
    * @return
    */
  inline def find(inline t: Term[P], at: TimeCoordinates): DIO[Iterable[JournalEntry[P]]]

  /** Same behavior as find, but it includes all the entities that have been already removed at the time indicated by the time
    * coordinates
    * @param t
    * @param at
    * @return
    */
  inline def findIncludingRemoved(inline t: Term[P], at: TimeCoordinates): DIO[Iterable[JournalEntry[P]]]

  /** Count the number of entities in the domain that exist at the given time coordinates (and have not been deleted)
    * @param at
    * @return
    */
  def countAll(at: TimeCoordinates): DIO[Long]

  /** Count the number of entities in the domain that exist at the given coordinates, have not been deleted and match the given
    * Term. It is an inline method to support Quill SQL code generation macros.
    *
    * @param t
    * @param at
    * @return
    */
  inline def count(inline t: Term[P], at: TimeCoordinates): DIO[Long]

  /** Create a new JournalEntry for the given Entity at the given time coordinates with the value provided in the payload.
    *
    * If the recorded time of the coordinates is earlier than the latest recorded time for the target entity, the update should
    * fail with a ValidationError.
    *
    * If the effective time for the coordinates is earlier than the latest effective time for the target entity, the update will:
    *
    *   - Fail for the initial "Linear" implementation of the Domain.
    *   - Create a new "branch" for the "Versioned" implementation of the Domain, creating copies of all records with a later
    *     effective time, with their recorded time updated to the given in the time coordinates.
    *
    * If the target entity has been deleted at the time coordinates, the update will fail with a ValidationError.
    *
    * @param eId
    * @param payload
    * @return
    */
  def update(eId: Id, at: TimeCoordinates, payload: P): DIO[JournalEntry[P]]

  /** Store the given JournalEntry if its coordinates are compatible with the given entity following the same rules that the @See
    * update with separate parameters
    * @param update
    * @return
    */
  def update(updated: JournalEntry[P]): DIO[JournalEntry[P]]

  /** Mark the entity with the given Id as removed at the time indicated by the time coordinates. A removed entity will:
    *
    *   - Not be retrieved by @See find for any time coordinates that are after the removal time.
    *   - Will have a Removal Journal Entry at the time of the removal call
    *   - Will be retrieved by @See findIncludingRemoved
    *
    * @param eId
    * @return
    */
  def remove(eId: Id, at: TimeCoordinates): DIO[JournalEntry[P]]

end JournaledDomain // trait
