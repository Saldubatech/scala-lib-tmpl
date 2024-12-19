package com.saldubatech.infrastructure.storage

import com.saldubatech.infrastructure.storage.JournaledDomain.EntryRecord
import com.saldubatech.lang.query.{Order, OrderTerm, Projectable}
import com.saldubatech.lang.types.AppResult
import io.getquill.*

trait ParametricDynamicSort[H, Q[_], QL[_]]:

  inline def apply(inline q: Q[H]): QL[Q[H]]

  inline def journaled(inline q: Q[EntryRecord[H & Payload]]): QL[Q[EntryRecord[H & Payload]]]

end ParametricDynamicSort // trait

trait ParametricDynamicSortFactory[H <: Product: Projectable, Q[_], QL[_], DS <: ParametricDynamicSort[H, Q, QL]]:
  def apply(o: Order): AppResult[DS]
end ParametricDynamicSortFactory // trait
