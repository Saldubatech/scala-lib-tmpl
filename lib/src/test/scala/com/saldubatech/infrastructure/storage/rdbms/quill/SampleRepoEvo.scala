package com.saldubatech.infrastructure.storage.rdbms.quill

import com.saldubatech.lang.Id
import com.saldubatech.infrastructure.storage.{DataRecord, PersistenceError, RepositoryError, Term}
import com.saldubatech.lang.types.{AppResult, DIO}
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.{IO, URLayer, ZEnvironment, ZIO, ZLayer}

import java.sql.SQLException
import javax.sql.DataSource

final case class ItemEvo(override val rId: Id, name: String, price: Double) extends DataRecord

object ItemService:

  val layer: URLayer[Quill.Postgres[Literal], ItemService] = ZLayer {
    for {
      quillCtx <- ZIO.service[Quill.Postgres[Literal]]
    } yield ItemService(quillCtx)
  }

end ItemService // object

class ItemService(quill: Quill.Postgres[Literal]) extends Domain.Service[ItemEvo] with Domain.ServiceInliner[ItemEvo](quill):

  import quill.*

  inline def baseQuery: Quoted[EntityQuery[ItemEvo]] = quote(querySchema[ItemEvo]("items_evo"))

  def addResolved(r: ItemEvo): ZIO[Any, AppResult.Error, ItemEvo] = add(r).provideEnvironment(ZEnvironment(ds))

  override def add(r: ItemEvo): DIO[ItemEvo] = adder(baseQuery)(r)

  override def get(rId: Id): DIO[ItemEvo] = getter(baseQuery)(rId)

  override def findAll: DIO[Iterable[ItemEvo]] = allFinder(baseQuery)

  override inline def find(inline t: Term[ItemEvo]): DIO[Iterable[ItemEvo]] = finder(baseQuery, t)

  override def dynamicFind(d: DynamicPredicate[ItemEvo]): DIO[Iterable[ItemEvo]] = dynamicFinder(baseQuery)(d)

  override def countAll: DIO[Long] = allCounter(baseQuery)

  override inline def count(inline t: Term[ItemEvo]): DIO[Long] = counter(baseQuery, t)

  override def dynamicCount(d: DynamicPredicate[ItemEvo]): DIO[Long] = dynamicCounter(baseQuery)(d)

  override def update(r: ItemEvo): DIO[ItemEvo] = updater(baseQuery)(r)

  override def remove(rId: Id): DIO[ItemEvo] = remover(baseQuery)(rId)

end ItemService // class
