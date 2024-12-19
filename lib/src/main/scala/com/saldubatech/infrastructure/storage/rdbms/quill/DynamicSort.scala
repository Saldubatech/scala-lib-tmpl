package com.saldubatech.infrastructure.storage.rdbms.quill

import com.saldubatech.infrastructure.storage.JournaledDomain.EntryRecord
import com.saldubatech.infrastructure.storage.{ParametricDynamicSort, ParametricDynamicSortFactory, Payload}
import com.saldubatech.lang.query.{Order, OrderDirection, OrderTerm, Projectable}
import com.saldubatech.lang.types.*
import io.getquill.*

object DynamicSort:

end DynamicSort // object

class DynamicSort[H <: Product: Projectable] private[quill] (sqlOrderBy: String) extends ParametricDynamicSort[H, Query, Quoted]:

  inline def apply(inline q: Query[H]): Quoted[Query[H]] = quote(sql"$q #$sqlOrderBy".pure.as[Query[H]])

  inline def journaled(inline q: Query[EntryRecord[H & Payload]]): Quoted[Query[EntryRecord[H & Payload]]] =
    quote(sql"$q #$sqlOrderBy".pure.as[Query[EntryRecord[H & Payload]]])

end DynamicSort

class DynamicSortFactory[H <: Product: Projectable] extends ParametricDynamicSortFactory[H, Query, Quoted, DynamicSort[H]]:

  def apply(s: Order): AppResult[DynamicSort[H]] = toOrderByUnsafe(s)

  private def toOrderByUnsafe(o: Order): AppResult[DynamicSort[H]] =
    o.terms.map { ot =>
      DynamicProjection.project(ot.locator).map(str => s"$str ${mapDirection(ot.direction)}")
    }.collectAll.map { terms =>
      DynamicSort[H](s"ORDER BY ${terms.mkString(", ")}")
    }

  private def mapDirection(d: OrderDirection): String =
    d match
      case OrderDirection.Asc  => "ASC"
      case OrderDirection.Desc => "DESC"

end DynamicSortFactory
