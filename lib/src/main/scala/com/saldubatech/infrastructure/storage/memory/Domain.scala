package com.saldubatech.infrastructure.storage.memory

import com.saldubatech.infrastructure.storage.{DataRecord, Domain as D, InsertionError, NotFoundError, Term}
import com.saldubatech.lang.Id
import com.saldubatech.lang.types.DIO
import zio.ZIO

import scala.util.chaining.scalaUtilChainingOps

object Domain:

  abstract class Service[R <: DataRecord](val storage: collection.mutable.Map[Id, R]) extends D.Service[R]:

    extension (r: Option[R]) private def resolveNotFound(id: Id): DIO[R] = r.fold(ZIO.fail(NotFoundError(id)))(ZIO.succeed(_))

    final override def add(r: R): DIO[R] =
      storage.get(r.rId) match
        case None           => ZIO.succeed(r.tap(storage += r.rId -> _))
        case Some(previous) => ZIO.fail(InsertionError(s"Cannot Insert record with duplicate Id: ${r.rId}"))

    final override def get(rId: Id): DIO[R] = storage.get(rId).resolveNotFound(rId)

    final override def findAll: DIO[Iterable[R]] = ZIO.succeed(storage.values.toSeq)

    final override inline def find(inline t: Term[R]): DIO[Iterable[R]] = ZIO.succeed(storage.values.filter(t(_)))

    final override def countAll: DIO[Long] = ZIO.succeed(storage.size)

    final override inline def count(inline t: Term[R]): DIO[Long] = ZIO.succeed(storage.values.count(t(_)))

    final override def update(r: R): DIO[R] =
      storage.get(r.rId) match
        case None           => ZIO.fail(NotFoundError(r.rId))
        case Some(previous) => ZIO.succeed(r.tap(storage += r.rId -> _))

    final override def remove(rId: Id): DIO[R] = storage.remove(rId).resolveNotFound(rId)

  end Service // class
end Domain    // object

class Domain
