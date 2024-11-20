package com.saldubatech.infrastructure.storage

import com.saldubatech.lang.types.*
import com.saldubatech.lang.types.datetime.{Duration, Epoch}

/** The temporal coordinates in a bi-temporal plane.
  *
  * @param recordedAt
  * @param effectiveAt
  */
case class TimeCoordinates(recordedAt: Epoch, effectiveAt: Epoch):

  /** A point (reference point) in the plane is visible from another point (view point) if and only if:
    *
    *   - The reference point is recorded before the view point
    *   - The reference point effective time is before the view point effective time
    *
    * @param viewpoint
    * @return
    */
  def isVisibleFrom(viewpoint: TimeCoordinates): Boolean =
    viewpoint.recordedAt >= this.recordedAt && viewpoint.effectiveAt >= this.effectiveAt

  def plusEffective(incr: Duration): TimeCoordinates = copy(effectiveAt = effectiveAt + incr)
  def plusRecorded(incr: Duration): TimeCoordinates  = copy(recordedAt = recordedAt + incr)

  def plus(recorded: Duration, effective: Duration): TimeCoordinates =
    TimeCoordinates(recordedAt + recorded, effectiveAt + effective)

  def plus(other: TimeCoordinates): TimeCoordinates =
    TimeCoordinates(recordedAt + other.recordedAt, effectiveAt + other.effectiveAt)

end TimeCoordinates // case class

object TimeCoordinates:

  class Ordering extends scala.math.Ordering[TimeCoordinates]:

    override def compare(x: TimeCoordinates, y: TimeCoordinates): Int =
      (x.recordedAt.compare(y.recordedAt), x.effectiveAt.compare(y.effectiveAt)) match
        case (0, e) => e
        case (r, _) => r

  given Ordering()

  def now: TimeCoordinates = TimeCoordinates(Epoch.now, Epoch.now)

  def origin: TimeCoordinates = TimeCoordinates(Epoch.zero, Epoch.zero)

end TimeCoordinates // object
