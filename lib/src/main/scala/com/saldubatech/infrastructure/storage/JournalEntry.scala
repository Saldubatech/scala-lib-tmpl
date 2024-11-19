package com.saldubatech.infrastructure.storage

import com.saldubatech.lang.Id
import com.saldubatech.lang.types.AppResult

type Payload = Product & Serializable

sealed trait JournalEntry[P <: Payload] extends DataRecord:

  val eId: Id
  val journalId: Id
  val coordinates: TimeCoordinates
  val payload: P

  import JournalEntry.*

  def update(payload: P, tc: TimeCoordinates): AppResult[Update[P]] =
    this match
      case rm: Removal[P] => AppResult.fail("Cannot Update an already removed record")
      case other =>
        if tc.recordedAt <= coordinates.recordedAt then AppResult.fail("RecordedAt times must be monotonic on updates")
        else AppResult.Success(Update(Id, journalId, eId, tc, payload, this.rId))

  def removal(tc: TimeCoordinates): AppResult[Removal[P]] =
    this match
      case rm: Removal[P] => AppResult.fail("Cannot Remove an already removed record")
      case other =>
        if tc.recordedAt <= coordinates.recordedAt then AppResult.fail("RecordedAt times must be monotonic on removals")
        else AppResult.Success(Removal(Id, journalId, eId, tc, payload, this.rId))

end JournalEntry // trait

object JournalEntry:

  val CREATION = "creation"
  val UPDATE   = "update"
  val REMOVAL  = "removal"

  case class Creation[P <: Payload](
      override val rId: Id,
      override val journalId: Id,
      override val eId: Id,
      override val coordinates: TimeCoordinates,
      override val payload: P)
      extends JournalEntry[P]

  case class Update[P <: Payload](
      override val rId: Id,
      override val journalId: Id,
      override val eId: Id,
      override val coordinates: TimeCoordinates,
      override val payload: P,
      previousEntry: Id)
      extends JournalEntry[P]

  case class Removal[P <: Payload](
      override val rId: Id,
      override val journalId: Id,
      override val eId: Id,
      override val coordinates: TimeCoordinates,
      override val payload: P,
      previousEntry: Id)
      extends JournalEntry[P]

end JournalEntry // object
